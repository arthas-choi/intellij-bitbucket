// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.extensions

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.plugins.template.authentication.accounts.AccountTokenChangedListener
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccountManager


import java.util.concurrent.ConcurrentHashMap

class BitbucketAccountGitAuthenticationFailureManager {
  private val storeMap = ConcurrentHashMap<BitbucketAccount, Set<String>>()

  init {
    ApplicationManager.getApplication().messageBus
      .connect()
      .subscribe(BitbucketAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC, object : AccountTokenChangedListener {
        override fun tokenChanged(account: BitbucketAccount) {
          storeMap.remove(account)
        }
      })
  }

  fun ignoreAccount(url: String, account: BitbucketAccount) {
    storeMap.compute(account) { _, current -> current?.plus(url) ?: setOf(url) }
  }

  fun isAccountIgnored(url: String, account: BitbucketAccount): Boolean = storeMap[account]?.contains(url) ?: false
}
