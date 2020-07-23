// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import git4idea.DialogManager
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import org.jetbrains.plugins.template.api.BitbucketServerPath
import org.jetbrains.plugins.template.authentication.BitbucketAuthenticationManager
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount
import org.jetbrains.plugins.template.authentication.ui.BitbucketChooseAccountDialog
import org.jetbrains.plugins.template.util.BitbucketAccountsMigrationHelper

import javax.swing.Icon

/**
 * If it is not possible to automatically determine suitable account, [GithubChooseAccountDialog] dialog will be shown.
 */
abstract class AbstractAuthenticatingBitbucketUrlGroupingAction(text: String?, description: String?, icon: Icon?)
  : AbstractBitbucketUrlGroupingAction(text, description, icon) {

  override fun actionPerformed(e: AnActionEvent, project: Project, repository: GitRepository, remote: GitRemote, remoteUrl: String) {
    if (!service<BitbucketAccountsMigrationHelper>().migrate(project)) return
    val account = getAccount(project, remoteUrl) ?: return
    actionPerformed(e, project, repository, remote, remoteUrl, account)
  }

  private fun getAccount(project: Project, remoteUrl: String): BitbucketAccount? {
    val authenticationManager = service<BitbucketAuthenticationManager>()
    val accounts = authenticationManager.getAccounts().filter { it.server.matches(remoteUrl) }
    //only possible when remote is on github.com
    if (accounts.isEmpty()) {
      if (!BitbucketServerPath.DEFAULT_SERVER.matches(remoteUrl))
        throw IllegalArgumentException("Remote $remoteUrl does not match ${BitbucketServerPath.DEFAULT_SERVER}")
      return authenticationManager.requestNewAccountForServer(BitbucketServerPath.DEFAULT_SERVER, project)
    }

    return accounts.singleOrNull()
           ?: accounts.find { it == authenticationManager.getDefaultAccount(project) }
           ?: chooseAccount(project, authenticationManager, remoteUrl, accounts)
  }

  private fun chooseAccount(project: Project, authenticationManager: BitbucketAuthenticationManager,
                            remoteUrl: String, accounts: List<BitbucketAccount>): BitbucketAccount? {
    val dialog = BitbucketChooseAccountDialog(project,
                                           null,
                                           accounts,
                                           "Choose GitHub account for: $remoteUrl",
                                           false,
                                           true)
    DialogManager.show(dialog)
    if (!dialog.isOK) return null
    val account = dialog.account
    if (dialog.setDefault) authenticationManager.setDefaultAccount(project, account)
    return account
  }

  protected abstract fun actionPerformed(e: AnActionEvent,
                                         project: Project,
                                         repository: GitRepository,
                                         remote: GitRemote,
                                         remoteUrl: String,
                                         account: BitbucketAccount)
}