// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.authentication

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import git4idea.DialogManager
import org.jetbrains.annotations.CalledInAny
import org.jetbrains.annotations.CalledInAwt
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutor
import org.jetbrains.plugins.template.api.BitbucketServerPath
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccountManager
import org.jetbrains.plugins.template.authentication.accounts.BitbucketProjectDefaultAccountHolder
import org.jetbrains.plugins.template.authentication.ui.BitbucketLoginDialog


import java.awt.Component

/**
 * Entry point for interactions with Bitbucket authentication subsystem
 */
class BitbucketAuthenticationManager internal constructor() {
  private val accountManager: BitbucketAccountManager
    get() = service()

  @CalledInAny
  fun hasAccounts() = accountManager.accounts.isNotEmpty()

  @CalledInAny
  fun getAccounts(): Set<BitbucketAccount> = accountManager.accounts

  @CalledInAny
  internal fun getTokenForAccount(account: BitbucketAccount): String? = accountManager.getTokenForAccount(account)

  @CalledInAwt
  @JvmOverloads
  internal fun requestNewToken(account: BitbucketAccount, project: Project?, parentComponent: Component? = null): String? {
    val dialog = BitbucketLoginDialog(BitbucketApiRequestExecutor.Factory.getInstance(), project, parentComponent, message = "Missing access token for $account")
      .withServer(account.server.toString(), false)
      .withCredentials(account.name)
      .withToken()

    DialogManager.show(dialog)
    if (!dialog.isOK) return null

    val token = dialog.getToken()
    account.name = dialog.getLogin()
    accountManager.updateAccountToken(account, token)
    return token
  }

  @CalledInAwt
  @JvmOverloads
  fun requestNewAccount(project: Project?, parentComponent: Component? = null): BitbucketAccount? {
    val dialog = BitbucketLoginDialog(BitbucketApiRequestExecutor.Factory.getInstance(), project, parentComponent, ::isAccountUnique)
    DialogManager.show(dialog)
    if (!dialog.isOK) return null

    return registerAccount(dialog.getLogin(), dialog.getServer(), dialog.getToken())
  }

  @CalledInAwt
  @JvmOverloads
  fun requestNewAccountForServer(server: BitbucketServerPath, project: Project?, parentComponent: Component? = null): BitbucketAccount? {
    val dialog = BitbucketLoginDialog(BitbucketApiRequestExecutor.Factory.getInstance(), project, parentComponent, ::isAccountUnique).withServer(server.toUrl(), false)
    DialogManager.show(dialog)
    if (!dialog.isOK) return null

    return registerAccount(dialog.getLogin(), dialog.getServer(), dialog.getToken())
  }

  @CalledInAwt
  @JvmOverloads
  fun requestNewAccountForServer(server: BitbucketServerPath,
                                 login: String,
                                 project: Project?,
                                 parentComponent: Component? = null): BitbucketAccount? {
    val dialog = BitbucketLoginDialog(BitbucketApiRequestExecutor.Factory.getInstance(), project, parentComponent, ::isAccountUnique)
      .withServer(server.toUrl(), false)
      .withCredentials(login, editableLogin = false)
    DialogManager.show(dialog)
    if (!dialog.isOK) return null

    return registerAccount(dialog.getLogin(), dialog.getServer(), dialog.getToken())
  }

  internal fun isAccountUnique(name: String,
                               server: BitbucketServerPath) = accountManager.accounts.none { it.name == name && it.server == server }

  @CalledInAwt
  @JvmOverloads
  fun requestReLogin(account: BitbucketAccount, project: Project?, parentComponent: Component? = null): Boolean {
    val dialog = BitbucketLoginDialog(BitbucketApiRequestExecutor.Factory.getInstance(), project, parentComponent)
      .withServer(account.server.toString(), false)
      .withCredentials(account.name)

    DialogManager.show(dialog)
    if (!dialog.isOK) return false

    val token = dialog.getToken()
    account.name = dialog.getLogin()
    accountManager.updateAccountToken(account, token)
    return true
  }

  @CalledInAwt
  internal fun removeAccount(githubAccount: BitbucketAccount) {
    accountManager.accounts -= githubAccount
  }

  @CalledInAwt
  internal fun updateAccountToken(account: BitbucketAccount, newToken: String) {
    accountManager.updateAccountToken(account, newToken)
  }

  private fun registerAccount(name: String, server: BitbucketServerPath, token: String): BitbucketAccount {
    val account = BitbucketAccountManager.createAccount(name, server)
    accountManager.accounts += account
    accountManager.updateAccountToken(account, token)
    return account
  }

  @CalledInAwt
  internal fun registerAccount(name: String, host: String, token: String): BitbucketAccount {
    return registerAccount(name, BitbucketServerPath.from(host), token)
  }

  @TestOnly
  fun clearAccounts() {
    for (account in accountManager.accounts) accountManager.updateAccountToken(account, null)
    accountManager.accounts = emptySet()
  }

  fun getDefaultAccount(project: Project): BitbucketAccount? {
    return project.service<BitbucketProjectDefaultAccountHolder>().account
  }

  @TestOnly
  fun setDefaultAccount(project: Project, account: BitbucketAccount?) {
    project.service<BitbucketProjectDefaultAccountHolder>().account = account
  }

  @CalledInAwt
  @JvmOverloads
  fun ensureHasAccounts(project: Project?, parentComponent: Component? = null): Boolean {
    if (!hasAccounts()) {
      if (requestNewAccount(project, parentComponent) == null) {
        return false
      }
    }
    return true
  }

  fun getSingleOrDefaultAccount(project: Project): BitbucketAccount? {
    project.service<BitbucketProjectDefaultAccountHolder>().account?.let { return it }
    val accounts = accountManager.accounts
    if (accounts.size == 1) return accounts.first()
    return null
  }

  companion object {
    @JvmStatic
    fun getInstance(): BitbucketAuthenticationManager {
      return service()
    }
  }
}
