// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.authentication.accounts

import com.google.common.cache.CacheBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInBackground
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutor
import org.jetbrains.plugins.template.api.BitbucketApiRequests
import org.jetbrains.plugins.template.api.data.BitbucketAuthenticatedUser

import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Loads the account information or provides it from cache
 * TODO: more abstraction
 */
class BitbucketAccountInformationProvider {

  private val informationCache = CacheBuilder.newBuilder()
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .build<BitbucketAccount, BitbucketAuthenticatedUser>()

  init {
    ApplicationManager.getApplication().messageBus
      .connect()
      .subscribe(BitbucketAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC, object : AccountTokenChangedListener {
        override fun tokenChanged(account: BitbucketAccount) {
          informationCache.invalidate(account)
        }
      })
  }

  @CalledInBackground
  @Throws(IOException::class)
  fun getInformation(executor: BitbucketApiRequestExecutor, indicator: ProgressIndicator, account: BitbucketAccount): BitbucketAuthenticatedUser {
    return informationCache.get(account) { executor.execute(indicator, BitbucketApiRequests.CurrentUser.get(account.server)) }
  }

  companion object {
    @JvmStatic
    fun getInstance(): BitbucketAccountInformationProvider {
      return service()
    }
  }
}