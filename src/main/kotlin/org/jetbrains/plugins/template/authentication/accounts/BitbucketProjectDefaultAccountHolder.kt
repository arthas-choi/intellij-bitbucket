// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.authentication.accounts

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.template.util.BitbucketNotifications


/**
 * Handles default Github account for project
 *
 * TODO: auto-detection
 */
@State(name = "BitbucketDefaultAccount", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
internal class BitbucketProjectDefaultAccountHolder(private val project: Project) : PersistentStateComponent<AccountState> {
  var account: BitbucketAccount? = null

  init {
    ApplicationManager.getApplication().messageBus.connect(project).subscribe(BitbucketAccountManager.ACCOUNT_REMOVED_TOPIC, object : AccountRemovedListener {
        override fun accountRemoved(removedAccount: BitbucketAccount) {
          if (account == removedAccount) account = null
        }
      })
  }

  override fun getState(): AccountState {
    return AccountState().apply { defaultAccountId = account?.id }
  }

  override fun loadState(state: AccountState) {
    account = state.defaultAccountId?.let(::findAccountById)
  }

  private fun findAccountById(id: String): BitbucketAccount? {
    val account = service<BitbucketAccountManager>().accounts.find { it.id == id }
    if (account == null) runInEdt {
      BitbucketNotifications.showWarning(project, "Missing Default GitHub Account", "",
              BitbucketNotifications.getConfigureAction(project))
    }
    return account
  }
}

internal class AccountState {
  var defaultAccountId: String? = null
}

