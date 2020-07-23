// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.extensions;

import com.intellij.dvcs.hosting.RepositoryListLoader;
import com.intellij.dvcs.hosting.RepositoryListLoadingException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import git4idea.remote.GitHttpAuthDataProvider;
import git4idea.remote.GitRepositoryHostingService;
import git4idea.remote.InteractiveGitHttpAuthDataProvider;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.CalledInBackground;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutor;
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutorManager;
import org.jetbrains.plugins.template.api.BitbucketApiRequests;
import org.jetbrains.plugins.template.api.BitbucketServerPath;
import org.jetbrains.plugins.template.api.data.BitbucketRepo;
import org.jetbrains.plugins.template.api.util.BitbucketApiPagesLoader;
import org.jetbrains.plugins.template.authentication.BitbucketAuthenticationManager;
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount;
import org.jetbrains.plugins.template.exceptions.BitbucketAuthenticationException;
import org.jetbrains.plugins.template.exceptions.BitbucketMissingTokenException;
import org.jetbrains.plugins.template.exceptions.BitbucketStatusCodeException;
import org.jetbrains.plugins.template.util.BitbucketAccountsMigrationHelper;
import org.jetbrains.plugins.template.util.BitbucketGitHelper;
import org.jetbrains.plugins.template.util.BitbucketUtil;


import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class BitbucketRepositoryHostingService extends GitRepositoryHostingService {
  @NotNull private final BitbucketAuthenticationManager myAuthenticationManager;
  @NotNull private final BitbucketApiRequestExecutorManager myExecutorManager;
  @NotNull private final BitbucketGitHelper myGitHelper;
  @NotNull private final BitbucketHttpAuthDataProvider myAuthDataProvider;

  public BitbucketRepositoryHostingService() {
    myAuthenticationManager = BitbucketAuthenticationManager.getInstance();
    myExecutorManager = BitbucketApiRequestExecutorManager.getInstance();
    myGitHelper = BitbucketGitHelper.getInstance();
    myAuthDataProvider = GitHttpAuthDataProvider.EP_NAME.findExtensionOrFail(BitbucketHttpAuthDataProvider.class);
  }

  @NotNull
  @Override
  public String getServiceDisplayName() {
    return BitbucketUtil.SERVICE_DISPLAY_NAME;
  }

  @Override
  @NotNull
  public RepositoryListLoader getRepositoryListLoader(@NotNull Project project) {
    return new RepositoryListLoader() {
      @NotNull private final Map<BitbucketAccount, BitbucketApiRequestExecutor> myExecutors = new HashMap<>();

      @Override
      public boolean isEnabled() {
        for (BitbucketAccount account : myAuthenticationManager.getAccounts()) {
          try {
            myExecutors.put(account, myExecutorManager.getExecutor(account));
          }
          catch (BitbucketMissingTokenException e) {
            // skip
          }
        }
        return !myExecutors.isEmpty();
      }

      @Override
      public boolean enable(@Nullable Component parentComponent) {
        if (!BitbucketAccountsMigrationHelper.getInstance().migrate(project, parentComponent)) return false;
        if (!myAuthenticationManager.ensureHasAccounts(project, parentComponent)) return false;
        boolean atLeastOneHasToken = false;
        for (BitbucketAccount account : myAuthenticationManager.getAccounts()) {
          BitbucketApiRequestExecutor executor = myExecutorManager.getExecutor(account, project);
          if (executor == null) continue;
          myExecutors.put(account, executor);
          atLeastOneHasToken = true;
        }
        return atLeastOneHasToken;
      }

      @NotNull
      @Override
      public Result getAvailableRepositoriesFromMultipleSources(@NotNull ProgressIndicator progressIndicator) {
        List<String> urls = new ArrayList<>();
        List<RepositoryListLoadingException> exceptions = new ArrayList<>();

        for (Map.Entry<BitbucketAccount, BitbucketApiRequestExecutor> entry : myExecutors.entrySet()) {
          BitbucketServerPath server = entry.getKey().getServer();
          BitbucketApiRequestExecutor executor = entry.getValue();
          try {
            Stream<BitbucketRepo> streamAssociated = BitbucketApiPagesLoader
              .loadAll(executor, progressIndicator, BitbucketApiRequests.CurrentUser.Repos.pages(server)).stream();

            Stream<BitbucketRepo> streamWatched = StreamEx.empty();
            try {
              streamWatched = BitbucketApiPagesLoader
                .loadAll(executor, progressIndicator, BitbucketApiRequests.CurrentUser.RepoSubs.pages(server)).stream();
            }
            catch (BitbucketAuthenticationException | BitbucketStatusCodeException ignore) {
              // We already can return something useful from getUserRepos, so let's ignore errors.
              // One of this may not exist in GitHub enterprise
            }
            urls.addAll(
              Stream.concat(streamAssociated, streamWatched)
                .sorted(Comparator.comparing(BitbucketRepo::getUserName).thenComparing(BitbucketRepo::getName))
                .map(repo -> myGitHelper.getRemoteUrl(server, repo.getUserName(), repo.getName()))
                .collect(Collectors.toList())
            );
          }
          catch (Exception e) {
            exceptions.add(new RepositoryListLoadingException("Cannot load repositories from GitHub", e));
          }
        }
        return new Result(urls, exceptions);
      }
    };
  }

  @CalledInBackground
  @Nullable
  @Override
  public InteractiveGitHttpAuthDataProvider getInteractiveAuthDataProvider(@NotNull Project project, @NotNull String url) {
    return getProvider(project, url, null);
  }

  @CalledInBackground
  @Nullable
  @Override
  public InteractiveGitHttpAuthDataProvider getInteractiveAuthDataProvider(@NotNull Project project,
                                                                           @NotNull String url,
                                                                           @NotNull String login) {
    return getProvider(project, url, login);
  }

  @Nullable
  private InteractiveGitHttpAuthDataProvider getProvider(@NotNull Project project, @NotNull String url, @Nullable String login) {
    Set<BitbucketAccount> potentialAccounts = myAuthDataProvider.getSuitableAccounts(project, url, login);
    if (!potentialAccounts.isEmpty()) {
      return new InteractiveSelectBitbucketAccountHttpAuthDataProvider(project, potentialAccounts, myAuthenticationManager);
    }

    if (BitbucketServerPath.DEFAULT_SERVER.matches(url)) {
      return new InteractiveCreateBitbucketAccountHttpAuthDataProvider(project, myAuthenticationManager,
                                                                    BitbucketServerPath.DEFAULT_SERVER, login);
    }

    return null;
  }
}
