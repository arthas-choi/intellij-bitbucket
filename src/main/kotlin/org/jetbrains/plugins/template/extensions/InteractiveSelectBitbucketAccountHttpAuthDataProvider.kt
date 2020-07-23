// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.extensions

import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.DialogManager
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import org.jetbrains.annotations.CalledInAwt
import org.jetbrains.plugins.template.authentication.BitbucketAuthenticationManager
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount
import org.jetbrains.plugins.template.authentication.ui.BitbucketChooseAccountDialog
import org.jetbrains.plugins.template.util.BitbucketUtil


import java.awt.Component

internal class InteractiveSelectBitbucketAccountHttpAuthDataProvider(private val project: Project,
                                                                     private val potentialAccounts: Collection<BitbucketAccount>,
                                                                     private val authenticationManager: BitbucketAuthenticationManager) : InteractiveGitHttpAuthDataProvider {

  @CalledInAwt
  override fun getAuthData(parentComponent: Component?): AuthData? {
    val dialog = BitbucketChooseAccountDialog(project,
                                           parentComponent,
                                           potentialAccounts,
                                           null,
                                           false,
                                           true,
                                           "Choose GitHub Account",
                                           "Log In")
    DialogManager.show(dialog)
    if (!dialog.isOK) return null
    val account = dialog.account
    val token = authenticationManager.getTokenForAccount(account)
                ?: authenticationManager.requestNewToken(account, project, parentComponent)
                ?: return null
    if (dialog.setDefault) authenticationManager.setDefaultAccount(project, account)
    return BitbucketHttpAuthDataProvider.BitbucketAccountAuthData(account, BitbucketUtil.GIT_AUTH_PASSWORD_SUBSTITUTE, token)
  }
}