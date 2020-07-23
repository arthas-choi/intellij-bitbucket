// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.authentication.util

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.plugins.template.api.*
import org.jetbrains.plugins.template.api.data.BitbucketAuthenticatedUser


object BBSecurityUtil {
  private const val REPO_SCOPE = "repo"
  private const val GIST_SCOPE = "gist"
  private const val READ_ORG_SCOPE = "read:org"
  val MASTER_SCOPES = listOf(REPO_SCOPE, GIST_SCOPE, READ_ORG_SCOPE)

  const val DEFAULT_CLIENT_NAME = "Github Integration Plugin"

  @JvmStatic
  internal fun loadCurrentUserWithScopes(executor: BitbucketApiRequestExecutor,
                                         progressIndicator: ProgressIndicator,
                                         server: BitbucketServerPath): Pair<BitbucketAuthenticatedUser, String?> {
    var scopes: String? = null
    val details = executor.execute(progressIndicator,
                                   object : BitbucketApiRequest.Get.Json<BitbucketAuthenticatedUser>(
                                           BitbucketApiRequests.getUrl(server,
                                                   BitbucketApiRequests.CurrentUser.urlSuffix),
                                           BitbucketAuthenticatedUser::class.java) {
                                     override fun extractResult(response: BitbucketApiResponse): BitbucketAuthenticatedUser {
                                       scopes = response.findHeader("X-OAuth-Scopes")
                                       return super.extractResult(response)
                                     }
                                   }.withOperationName("get profile information"))
    return details to scopes
  }

  @JvmStatic
  internal fun isEnoughScopes(grantedScopes: String): Boolean {
    val scopesArray = grantedScopes.split(", ")
    if (scopesArray.isEmpty()) return false
    if (!scopesArray.contains(REPO_SCOPE)) return false
    if (!scopesArray.contains(GIST_SCOPE)) return false
    if (scopesArray.none { it.endsWith(":org") }) return false

    return true
  }
}