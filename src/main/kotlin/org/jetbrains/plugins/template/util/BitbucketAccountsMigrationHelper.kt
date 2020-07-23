// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.util

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import git4idea.DialogManager
import org.jetbrains.annotations.CalledInAwt
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutor
import org.jetbrains.plugins.template.api.BitbucketApiRequests
import org.jetbrains.plugins.template.api.BitbucketServerPath
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccountManager
import org.jetbrains.plugins.template.authentication.ui.BitbucketLoginDialog


import java.awt.Component
import java.io.IOException

internal const val GITHUB_SETTINGS_PASSWORD_KEY = "GITHUB_SETTINGS_PASSWORD_KEY"

/**
 * Temporary helper
 * Will move single-account authorization data to accounts list if it was a token-based auth and clear old settings
 */
@Suppress("DEPRECATION")
class BitbucketAccountsMigrationHelper {
  private val LOG = logger<BitbucketAccountsMigrationHelper>()

  internal fun getOldServer(): BitbucketServerPath? {
    try {
      if (hasOldAccount()) {
        return BitbucketServerPath.from(BitbucketSettings.getInstance().host ?: BitbucketServerPath.DEFAULT_HOST)
      }
    }
    catch (ignore: Exception) {
      // it could be called from AnAction.update()
    }
    return null
  }

  private fun hasOldAccount(): Boolean {
    // either password-based with specified login or token based
    val settings = BitbucketSettings.getInstance()
    return ((settings.authType == GithubAuthData.AuthType.BASIC && settings.login != null) ||
            (settings.authType == GithubAuthData.AuthType.TOKEN))
  }

  /**
   * @return false if process was cancelled by user, true otherwise
   */
  @CalledInAwt
  @JvmOverloads
  fun migrate(project: Project, parentComponent: Component? = null): Boolean {
    LOG.debug("Migrating old auth")
    val settings = BitbucketSettings.getInstance()
    val login = settings.login
    val host = settings.host
    val password = PasswordSafe.instance.getPassword(CredentialAttributes(BitbucketSettings::class.java, GITHUB_SETTINGS_PASSWORD_KEY))
    val authType = settings.authType
    LOG.debug("Old auth data: { login: $login, host: $host, authType: $authType, password null: ${password == null} }")

    val hasAnyInfo = login != null || host != null || authType != null || password != null
    if (!hasAnyInfo) return true

    var dialogCancelled = false

    if (service<BitbucketAccountManager>().accounts.isEmpty()) {
      val hostToUse = host ?: BitbucketServerPath.DEFAULT_HOST
      when (authType) {
        GithubAuthData.AuthType.TOKEN -> {
          LOG.debug("Migrating token auth")
          if (password != null) {
            val executorFactory = BitbucketApiRequestExecutor.Factory.getInstance()
            try {
              val server = BitbucketServerPath.from(hostToUse)
              val progressManager = ProgressManager.getInstance()
              val accountName = progressManager.runProcessWithProgressSynchronously(ThrowableComputable<String, IOException> {
                executorFactory.create(password).execute(progressManager.progressIndicator,
                        BitbucketApiRequests.CurrentUser.get(server)).login
              }, "Accessing GitHub", true, project)
              val account = BitbucketAccountManager.createAccount(accountName, server)
              registerAccount(account, password)
            }
            catch (e: Exception) {
              LOG.debug("Failed to migrate old token-based auth. Showing dialog.", e)
              val dialog = BitbucketLoginDialog(executorFactory, project, parentComponent)
                .withServer(hostToUse, false).withToken(password).withError(e)
              dialogCancelled = !registerFromDialog(dialog)
            }
          }
        }
        GithubAuthData.AuthType.BASIC -> {
          LOG.debug("Migrating basic auth")
          val dialog = BitbucketLoginDialog(BitbucketApiRequestExecutor.Factory.getInstance(), project, parentComponent,
                                         message = "Password authentication is no longer supported for Github.\n" +
                                                   "Personal access token can be acquired instead.")
            .withServer(hostToUse, false).withCredentials(login, password)
          dialogCancelled = !registerFromDialog(dialog)
        }
        else -> {
        }
      }
    }
    return !dialogCancelled
  }

  private fun registerFromDialog(dialog: BitbucketLoginDialog): Boolean {
    DialogManager.show(dialog)
    return if (dialog.isOK) {
      registerAccount(BitbucketAccountManager.createAccount(dialog.getLogin(), dialog.getServer()), dialog.getToken())
      true
    }
    else false
  }

  private fun registerAccount(account: BitbucketAccount, token: String) {
    val accountManager = service<BitbucketAccountManager>()
    accountManager.accounts += account
    accountManager.updateAccountToken(account, token)
    LOG.debug("Registered account $account")
  }

  companion object {
    @JvmStatic
    fun getInstance(): BitbucketAccountsMigrationHelper = service()
  }
}
