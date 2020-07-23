// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.commands.*;
import git4idea.config.GitVcsSettings;
import git4idea.config.GitSaveChangesPolicy;
import git4idea.rebase.GitRebaseProblemDetector;
import git4idea.rebase.GitRebaser;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.update.GitUpdateResult;
import git4idea.util.GitPreservingProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.template.api.*;
import org.jetbrains.plugins.template.api.data.BitbucketRepo;
import org.jetbrains.plugins.template.api.data.BitbucketRepoDetailed;
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount;
import org.jetbrains.plugins.template.util.BitbucketGitHelper;
import org.jetbrains.plugins.template.util.BitbucketNotifications;
import org.jetbrains.plugins.template.util.BitbucketUrlUtil;
import org.jetbrains.plugins.template.util.BitbucketUtil;


import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector.Operation.CHECKOUT;
import static git4idea.fetch.GitFetchSupport.fetchSupport;

public class BitbucketRebaseAction extends AbstractAuthenticatingBitbucketUrlGroupingAction {
  private static final Logger LOG = BitbucketUtil.LOG;
  private static final String CANNOT_PERFORM_GITHUB_REBASE = "Can't perform GitHub rebase";
  private static final String UPSTREAM_REMOTE = "upstream";

  public BitbucketRebaseAction() {
    super("Rebase my GitHub fork", "Rebase your GitHub forked repository relative to the origin", AllIcons.Vcs.Vendors.Github);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e,
                              @NotNull Project project,
                              @NotNull GitRepository repository,
                              @NotNull GitRemote remote,
                              @NotNull String remoteUrl,
                              @NotNull BitbucketAccount account) {
    FileDocumentManager.getInstance().saveAllDocuments();
    BitbucketApiRequestExecutor executor = BitbucketApiRequestExecutorManager.getInstance().getExecutor(account, project);
    if (executor == null) return;

    BBRepositoryPath repoPath =BitbucketUrlUtil.getUserAndRepositoryFromRemoteUrl(remoteUrl);
    if (repoPath == null) {
      BitbucketNotifications.showError(project, CANNOT_PERFORM_GITHUB_REBASE, "Invalid Github remote: " + remoteUrl);
      return;
    }
    new RebaseTask(project, executor, Git.getInstance(), account.getServer(), repository, repoPath).queue();
  }

  private static class RebaseTask extends Task.Backgroundable {
    @NotNull private final BitbucketApiRequestExecutor myRequestExecutor;
    @NotNull private final Git myGit;
    @NotNull private final BitbucketServerPath myServer;
    @NotNull private final GitRepository myRepository;
    @NotNull private final BBRepositoryPath myRepoPath;

    RebaseTask(@NotNull Project project,
               @NotNull BitbucketApiRequestExecutor requestExecutor,
               @NotNull Git git,
               @NotNull BitbucketServerPath server,
               @NotNull GitRepository repository,
               @NotNull BBRepositoryPath repoPath) {
      super(project, "Rebasing GitHub Fork...");
      myRequestExecutor = requestExecutor;
      myGit = git;
      myServer = server;
      myRepository = repository;
      myRepoPath = repoPath;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
      myRepository.update();
      String upstreamRemoteUrl = findUpstreamRemoteUrl();
      if (upstreamRemoteUrl == null) {
        indicator.setText("Configuring upstream remote...");
        LOG.info("Configuring upstream remote");
        if ((upstreamRemoteUrl = configureUpstreamRemote(indicator)) == null) return;
      }

      BBRepositoryPath userAndRepo = BitbucketUrlUtil.getUserAndRepositoryFromRemoteUrl(upstreamRemoteUrl);
      if (userAndRepo == null) {
        BitbucketNotifications.showError(myProject, CANNOT_PERFORM_GITHUB_REBASE, "Can't validate upstream remote: " + upstreamRemoteUrl);
        return;
      }
      if (isUpstreamWithSameUsername(indicator, userAndRepo)) {
        BitbucketNotifications.showError(myProject, CANNOT_PERFORM_GITHUB_REBASE,
                                      "Configured upstream seems to be your own repository: " + upstreamRemoteUrl);
        return;
      }
      String name = getDefaultBranchName(indicator, userAndRepo);
      if (name == null) {
        return;
      }
      String onto = UPSTREAM_REMOTE + "/" + name;

      LOG.info("Fetching upstream");
      indicator.setText("Fetching upstream...");
      if (!fetchParent()) {
        return;
      }

      LOG.info("Rebasing current branch");
      indicator.setText("Rebasing current branch onto " + onto + "...");
      rebaseCurrentBranch(indicator, onto);
    }

    @Nullable
    private String findUpstreamRemoteUrl() {
      return myRepository.getRemotes().stream()
        .filter(remote -> remote.getName().equals("upstream") &&
                          remote.getFirstUrl() != null &&
                          myServer.matches(remote.getFirstUrl()))
        .findFirst()
        .map(GitRemote::getFirstUrl).orElse(null);
    }

    private boolean isUpstreamWithSameUsername(@NotNull ProgressIndicator indicator, @NotNull BBRepositoryPath userAndRepo) {
      try {
        String username = myRequestExecutor.execute(indicator, BitbucketApiRequests.CurrentUser.get(myServer)).getLogin();
        return userAndRepo.getOwner().equals(username);
      }
      catch (IOException e) {
        BitbucketNotifications.showError(myProject, CANNOT_PERFORM_GITHUB_REBASE, "Can't get user information");
        return true;
      }
    }

    @Nullable
    private String getDefaultBranchName(@NotNull ProgressIndicator indicator, @NotNull BBRepositoryPath userAndRepo) {
      try {
        BitbucketRepo repo = myRequestExecutor.execute(indicator,
                BitbucketApiRequests.Repos.get(myServer, userAndRepo.getOwner(), userAndRepo.getRepository()));
        if (repo == null) {
          BitbucketNotifications.showError(myProject, CANNOT_PERFORM_GITHUB_REBASE, "Can't retrieve upstream information for " + userAndRepo);
          return null;
        }
        return repo.getDefaultBranch();
      }
      catch (IOException e) {
        BitbucketNotifications
          .showError(myProject, CANNOT_PERFORM_GITHUB_REBASE, "Can't retrieve upstream information for " + userAndRepo, e.getMessage());
        return null;
      }
    }

    @Nullable
    private String configureUpstreamRemote(@NotNull ProgressIndicator indicator) {
      BitbucketRepoDetailed repositoryInfo = loadRepositoryInfo(indicator, myRepoPath);
      if (repositoryInfo == null) {
        return null;
      }

      if (!repositoryInfo.isFork() || repositoryInfo.getParent() == null) {
        BitbucketNotifications
          .showWarningURL(myProject, CANNOT_PERFORM_GITHUB_REBASE, "GitHub repository ", "'" + repositoryInfo.getName() + "'",
                          " is not a fork", repositoryInfo.getHtmlUrl());
        return null;
      }

      String parentRepoUrl = BitbucketGitHelper.getInstance().getRemoteUrl(myServer, repositoryInfo.getParent().getFullPath());

      LOG.info("Adding GitHub parent as a remote host");
      indicator.setText("Adding GitHub parent as a remote host...");
      try {
        myGit.addRemote(myRepository, UPSTREAM_REMOTE, parentRepoUrl).throwOnError();
      }
      catch (VcsException e) {
        BitbucketNotifications
          .showError(myProject, CANNOT_PERFORM_GITHUB_REBASE, "Could not configure \"" + UPSTREAM_REMOTE + "\" remote:\n" + e.getMessage());
        return null;
      }
      myRepository.update();
      return parentRepoUrl;
    }

    @Nullable
    private BitbucketRepoDetailed loadRepositoryInfo(@NotNull ProgressIndicator indicator, @NotNull BBRepositoryPath fullPath) {
      try {
        BitbucketRepoDetailed repo =
          myRequestExecutor.execute(indicator, BitbucketApiRequests.Repos.get(myServer, fullPath.getOwner(), fullPath.getRepository()));
        if (repo == null) BitbucketNotifications.showError(myProject, "Repository " + fullPath.toString() + " was not found", "");
        return repo;
      }
      catch (IOException e) {
        BitbucketNotifications.showError(myProject, "Can't load repository info", e);
        return null;
      }
    }

    private boolean fetchParent() {
      GitRemote remote = GitUtil.findRemoteByName(myRepository, UPSTREAM_REMOTE);
      if (remote == null) {
        LOG.warn("Couldn't find remote " + " remoteName " + " in " + myRepository);
        return false;
      }
      return fetchSupport(myProject).fetch(myRepository, remote).showNotificationIfFailed();
    }

    private void rebaseCurrentBranch(@NotNull ProgressIndicator indicator, String onto) {
      try (AccessToken ignore = DvcsUtil.workingTreeChangeStarted(myProject, "Rebase")) {
        List<VirtualFile> rootsToSave = Collections.singletonList(myRepository.getRoot());
        GitSaveChangesPolicy saveMethod = GitVcsSettings.getInstance(myProject).getSaveChangesPolicy();
        GitPreservingProcess process = new GitPreservingProcess(myProject, myGit, rootsToSave, "Rebasing", onto,
                                                                saveMethod, indicator,
                                                                () -> doRebaseCurrentBranch(indicator, onto));
        process.execute();
      }
    }

    private void doRebaseCurrentBranch(@NotNull ProgressIndicator indicator, String onto) {
      GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(myProject);
      GitRebaser rebaser = new GitRebaser(myProject, myGit, indicator);
      VirtualFile root = myRepository.getRoot();

      GitLineHandler handler = new GitLineHandler(myProject, root, GitCommand.REBASE);
      handler.setStdoutSuppressed(false);
      handler.addParameters(onto);

      final GitRebaseProblemDetector rebaseConflictDetector = new GitRebaseProblemDetector();
      handler.addLineListener(rebaseConflictDetector);

      final GitUntrackedFilesOverwrittenByOperationDetector untrackedFilesDetector =
        new GitUntrackedFilesOverwrittenByOperationDetector(root);
      final GitLocalChangesWouldBeOverwrittenDetector localChangesDetector = new GitLocalChangesWouldBeOverwrittenDetector(root, CHECKOUT);
      handler.addLineListener(untrackedFilesDetector);
      handler.addLineListener(localChangesDetector);
      handler.addLineListener(GitStandardProgressAnalyzer.createListener(indicator));

      String oldText = indicator.getText();
      indicator.setText("Rebasing onto " + onto + "...");
      GitCommandResult rebaseResult = myGit.runCommand(handler);
      indicator.setText(oldText);
      repositoryManager.updateRepository(root);
      if (rebaseResult.success()) {
        root.refresh(false, true);
        BitbucketNotifications.showInfo(myProject, "Success", "Successfully rebased GitHub fork");
      }
      else {
        GitUpdateResult result = rebaser.handleRebaseFailure(handler, root, rebaseResult, rebaseConflictDetector,
                                                             untrackedFilesDetector, localChangesDetector);
        if (result == GitUpdateResult.NOTHING_TO_UPDATE ||
            result == GitUpdateResult.SUCCESS ||
            result == GitUpdateResult.SUCCESS_WITH_RESOLVED_CONFLICTS) {
          BitbucketNotifications.showInfo(myProject, "Success", "Successfully rebased GitHub fork");
        }
      }
    }
  }
}
