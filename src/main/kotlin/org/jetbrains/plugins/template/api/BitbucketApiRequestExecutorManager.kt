// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.CalledInAwt
import org.jetbrains.plugins.template.authentication.BitbucketAuthenticationManager
import org.jetbrains.plugins.template.authentication.accounts.AccountTokenChangedListener
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccountManager
import org.jetbrains.plugins.template.exceptions.BitbucketMissingTokenException


import java.awt.Component

/**
 * Allows to acquire API executor without exposing the auth token to external code
 */
class BitbucketApiRequestExecutorManager : AccountTokenChangedListener {
  private val executors = mutableMapOf<BitbucketAccount, BitbucketApiRequestExecutor.WithTokenAuth>()

  init {
    ApplicationManager.getApplication().messageBus
      .connect()
      .subscribe(BitbucketAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC, this)
  }

  companion object {
    @JvmStatic
    fun getInstance(): BitbucketApiRequestExecutorManager = service()
  }

  override fun tokenChanged(account: BitbucketAccount) {
    val token = service<BitbucketAccountManager>().getTokenForAccount(account)
    if (token == null) executors.remove(account)
    else executors[account]?.token = token
  }

  @CalledInAwt
  fun getExecutor(account: BitbucketAccount, project: Project): BitbucketApiRequestExecutor.WithTokenAuth? {
    return getOrTryToCreateExecutor(account) { BitbucketAuthenticationManager.getInstance().requestNewToken(account, project) }
  }

  @CalledInAwt
  fun getExecutor(account: BitbucketAccount, parentComponent: Component): BitbucketApiRequestExecutor.WithTokenAuth? {
    return getOrTryToCreateExecutor(account) { BitbucketAuthenticationManager.getInstance().requestNewToken(account, null, parentComponent) }
  }

  @CalledInAwt
  @Throws(BitbucketMissingTokenException::class)
  fun getExecutor(account: BitbucketAccount): BitbucketApiRequestExecutor.WithTokenAuth {
    return getOrTryToCreateExecutor(account) { throw BitbucketMissingTokenException(account) }!!
  }

  private fun getOrTryToCreateExecutor(account: BitbucketAccount,
                                       missingTokenHandler: () -> String?): BitbucketApiRequestExecutor.WithTokenAuth? {

    return executors.getOrPut(account) {
      (BitbucketAuthenticationManager.getInstance().getTokenForAccount(account) ?: missingTokenHandler())
        ?.let(BitbucketApiRequestExecutor.Factory.getInstance()::create) ?: return null
    }
  }
}