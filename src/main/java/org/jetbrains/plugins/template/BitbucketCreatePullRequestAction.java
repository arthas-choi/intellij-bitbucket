// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.DialogManager;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutor;
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutorManager;
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount;
import org.jetbrains.plugins.template.ui.BitbucketCreatePullRequestDialog;

/**
 * @author Aleksey Pivovarov
 */
public class BitbucketCreatePullRequestAction extends AbstractAuthenticatingBitbucketUrlGroupingAction {
  public BitbucketCreatePullRequestAction() {
    super("Create Pull Request", "Create pull request from current branch", AllIcons.Vcs.Vendors.Github);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e,
                              @NotNull Project project,
                              @NotNull GitRepository repository,
                              @NotNull GitRemote remote,
                              @NotNull String remoteUrl,
                              @NotNull BitbucketAccount account) {
    createPullRequest(project, repository, remote, remoteUrl, account);
  }

  static void createPullRequest(@NotNull Project project,
                                @NotNull GitRepository gitRepository,
                                @NotNull GitRemote remote,
                                @NotNull String remoteUrl,
                                @NotNull BitbucketAccount account) {
    BitbucketApiRequestExecutor executor = BitbucketApiRequestExecutorManager.getInstance().getExecutor(account, project);
    if (executor == null) return;

    BitbucketCreatePullRequestWorker worker = BitbucketCreatePullRequestWorker.create(project, gitRepository, remote, remoteUrl,
                                                                                executor, account.getServer());
    if (worker == null) {
      return;
    }

    BitbucketCreatePullRequestDialog dialog = new BitbucketCreatePullRequestDialog(project, worker);
    DialogManager.show(dialog);
  }
}