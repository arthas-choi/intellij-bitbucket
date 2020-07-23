// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.plugins.template.extensions

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.remote.GitHttpAuthDataProvider
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutor
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutorManager
import org.jetbrains.plugins.template.authentication.BitbucketAuthenticationManager
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccountInformationProvider

import java.io.IOException

private val LOG = logger<BitbucketHttpAuthDataProvider>()

class BitbucketHttpAuthDataProvider : GitHttpAuthDataProvider {
  override fun getAuthData(project: Project, url: String): BitbucketAccountAuthData? {
    return getSuitableAccounts(project, url, null).singleOrNull()?.let { account ->
      try {
        val token = BitbucketAuthenticationManager.getInstance().getTokenForAccount(account) ?: return null
        val username = service<BitbucketAccountInformationProvider>().getInformation(BitbucketApiRequestExecutor.Factory.getInstance().create(token),
                                                                 DumbProgressIndicator(),
                                                                 account).login
        BitbucketAccountAuthData(account, username, token)
      }
      catch (e: IOException) {
        LOG.info("Cannot load username for $account", e)
        null
      }
    }
  }

  override fun isSilent(): Boolean = true

  override fun getAuthData(project: Project, url: String, login: String): BitbucketAccountAuthData? {
    return getSuitableAccounts(project, url, login).singleOrNull()?.let { account ->
      return BitbucketAuthenticationManager.getInstance().getTokenForAccount(account)?.let { BitbucketAccountAuthData(account, login, it) }
    }
  }

  override fun forgetPassword(project: Project, url: String, authData: AuthData) {
    if (authData is BitbucketAccountAuthData) {
      project.service<BitbucketAccountGitAuthenticationFailureManager>().ignoreAccount(url, authData.account)
    }
  }

  fun getSuitableAccounts(project: Project, url: String, login: String?): Set<BitbucketAccount> {
    val authenticationFailureManager = project.service<BitbucketAccountGitAuthenticationFailureManager>()
    val authenticationManager = BitbucketAuthenticationManager.getInstance()
    var potentialAccounts = authenticationManager.getAccounts()
      .filter { it.server.matches(url) }
      .filter { !authenticationFailureManager.isAccountIgnored(url, it) }

    if (login != null) {
      potentialAccounts = potentialAccounts.filter {
        try {
          service<BitbucketAccountInformationProvider>().getInformation(BitbucketApiRequestExecutorManager.getInstance().getExecutor(it),
                                                    DumbProgressIndicator(),
                                                    it).login == login
        }
        catch (e: IOException) {
          LOG.info("Cannot load username for $it", e)
          false
        }
      }
    }

    val defaultAccount = authenticationManager.getDefaultAccount(project)
    if (defaultAccount != null && potentialAccounts.contains(defaultAccount)) return setOf(defaultAccount)
    return potentialAccounts.toSet()
  }

  class BitbucketAccountAuthData(val account: BitbucketAccount,
                                 login: String,
                                 password: String) : AuthData(login, password)
}