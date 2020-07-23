// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.extensions

import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import org.jetbrains.annotations.CalledInAwt
import org.jetbrains.plugins.template.api.BitbucketServerPath
import org.jetbrains.plugins.template.authentication.BitbucketAuthenticationManager
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount
import org.jetbrains.plugins.template.util.BitbucketUtil


import java.awt.Component

internal class InteractiveCreateBitbucketAccountHttpAuthDataProvider(private val project: Project,
                                                                     private val authenticationManager: BitbucketAuthenticationManager,
                                                                     private val serverPath: BitbucketServerPath,
                                                                     private val login: String? = null)
  : InteractiveGitHttpAuthDataProvider {

  @CalledInAwt
  override fun getAuthData(parentComponent: Component?): AuthData? {
    if (login == null) {
      val account = authenticationManager.requestNewAccountForServer(serverPath, project, parentComponent)
                    ?: return null
      val token = getToken(account, parentComponent) ?: return null
      return BitbucketHttpAuthDataProvider.BitbucketAccountAuthData(account, BitbucketUtil.GIT_AUTH_PASSWORD_SUBSTITUTE, token)
    }
    else {
      val account = authenticationManager.requestNewAccountForServer(serverPath, login, project, parentComponent)
                    ?: return null
      val token = getToken(account, parentComponent) ?: return null
      return BitbucketHttpAuthDataProvider.BitbucketAccountAuthData(account, login, token)
    }
  }

  private fun getToken(account: BitbucketAccount, parentComponent: Component?) =
    authenticationManager.getTokenForAccount(account) ?: authenticationManager.requestNewToken(account, project, parentComponent)
}