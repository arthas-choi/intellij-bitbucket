// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.ui

import com.intellij.ide.IdeBundle.message
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.*







class BitbucketSettingsConfigurable internal constructor(
  private val project: Project,
  private val settings: GithubSettings,
  private val accountManager: GithubAccountManager,
  private val defaultAccountHolder: GithubProjectDefaultAccountHolder,
  private val executorFactory: GithubApiRequestExecutor.Factory,
  private val avatarLoader: CachingGithubUserAvatarLoader,
  private val imageResizer: GithubImageResizer
) : BoundConfigurable(GithubUtil.SERVICE_DISPLAY_NAME, "settings.github") {

  override fun createPanel(): DialogPanel {
    return panel {
      row {
        val accountsPanel = GHAccountsPanel(project, executorFactory, avatarLoader, imageResizer).apply {
          Disposer.register(disposable!!, this)
        }
        component(accountsPanel)
          .onIsModified { accountsPanel.isModified(accountManager.accounts, defaultAccountHolder.account) }
          .onReset {
            val accountsMap = accountManager.accounts.associateWith { accountManager.getTokenForAccount(it) }
            accountsPanel.setAccounts(accountsMap, defaultAccountHolder.account)
            accountsPanel.clearNewTokens()
            accountsPanel.loadExistingAccountsDetails()
          }
          .onApply {
            val (accountsTokenMap, defaultAccount) = accountsPanel.getAccounts()
            accountManager.accounts = accountsTokenMap.keys
            accountsTokenMap.filterValues { it != null }.forEach(accountManager::updateAccountToken)
            defaultAccountHolder.account = defaultAccount
            accountsPanel.clearNewTokens()
          }

        ApplicationManager.getApplication().messageBus.connect(disposable!!)
          .subscribe(GithubAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC,
                     object : AccountTokenChangedListener {
                       override fun tokenChanged(account: GithubAccount) {
                         if (!isModified) reset()
                       }
                     })
      }
      row {
        checkBox(message("github.settings.clone.ssh"), settings::isCloneGitUsingSsh, settings::setCloneGitUsingSsh)
      }
      row {
        cell {
          label(message("github.settings.timeout"))
          intTextField({ settings.connectionTimeout / 1000 }, { settings.connectionTimeout = it * 1000 }, columns = 2, range = 0..60)
          label(message("github.settings.seconds"))
        }
      }
    }
  }
}