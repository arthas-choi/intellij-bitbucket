// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.authentication.accounts

import com.intellij.credentialStore.*
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.messages.Topic
import org.jetbrains.plugins.template.api.BitbucketServerPath
import org.jetbrains.plugins.template.util.BitbucketUtil

import kotlin.properties.Delegates.observable

/**
 * Handles application-level Github accounts
 */
@State(name = "BitbucketAccounts", storages = [
  Storage(value = "bitbucket.xml"),
  Storage(value = "bitbucket_settings.xml", deprecated = true)
])
internal class BitbucketAccountManager : PersistentStateComponent<Array<BitbucketAccount>> {
  var accounts: Set<BitbucketAccount> by observable(setOf()) { _, oldValue, newValue ->
    oldValue.filter { it !in newValue }.forEach(this::accountRemoved)
    LOG.debug("Account list changed to: $newValue")
  }

  init {
    ApplicationManager.getApplication().messageBus.connect()
      .subscribe(PasswordSafeSettings.TOPIC, object : PasswordSafeSettingsListener {
        override fun credentialStoreCleared() {
          val publisher = ApplicationManager.getApplication()
            .messageBus
            .syncPublisher(ACCOUNT_TOKEN_CHANGED_TOPIC)
          accounts.forEach(publisher::tokenChanged)
        }
      })
  }

  private fun accountRemoved(account: BitbucketAccount) {
    updateAccountToken(account, null)
    ApplicationManager.getApplication()
      .messageBus
      .syncPublisher(ACCOUNT_REMOVED_TOPIC).accountRemoved(account)
  }

  /**
   * Add/update/remove Github OAuth token from application
   */
  fun updateAccountToken(account: BitbucketAccount, token: String?) {
    PasswordSafe.instance.set(createCredentialAttributes(account.id), token?.let { createCredentials(account.id, it) })
    LOG.debug((if (token == null) "Cleared" else "Updated") + " OAuth token for account: $account")
    ApplicationManager.getApplication()
      .messageBus
      .syncPublisher(ACCOUNT_TOKEN_CHANGED_TOPIC).tokenChanged(account)
  }

  /**
   * Retrieve OAuth token for account from password safe
   */
  fun getTokenForAccount(account: BitbucketAccount): String? = PasswordSafe.instance.get(createCredentialAttributes(account.id))?.getPasswordAsString()

  override fun getState() = accounts.toTypedArray()

  override fun loadState(state: Array<BitbucketAccount>) {
    accounts = state.toHashSet()
  }

  companion object {
    private val LOG = logger<BitbucketAccountManager>()
    @JvmStatic
    val ACCOUNT_REMOVED_TOPIC = Topic("GITHUB_ACCOUNT_REMOVED", AccountRemovedListener::class.java)
    @JvmStatic
    val ACCOUNT_TOKEN_CHANGED_TOPIC = Topic("GITHUB_ACCOUNT_TOKEN_CHANGED", AccountTokenChangedListener::class.java)

    fun createAccount(name: String, server: BitbucketServerPath) = BitbucketAccount(name, server)
  }
}

private fun createCredentialAttributes(accountId: String) = CredentialAttributes(createServiceName(accountId))

private fun createCredentials(accountId: String, token: String) = Credentials(accountId, token)

private fun createServiceName(accountId: String): String = generateServiceName(BitbucketUtil.SERVICE_DISPLAY_NAME, accountId)

interface AccountRemovedListener {
  fun accountRemoved(removedAccount: BitbucketAccount)
}

interface AccountTokenChangedListener {
  fun tokenChanged(account: BitbucketAccount)
}