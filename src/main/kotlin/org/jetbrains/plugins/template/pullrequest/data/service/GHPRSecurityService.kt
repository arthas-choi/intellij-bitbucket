// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.data.service



interface GHPRSecurityService {
  val currentUser: GHUser

  fun isCurrentUser(user: GithubUser): Boolean

  fun currentUserHasPermissionLevel(level: GHRepositoryPermissionLevel): Boolean
  fun currentUserCanEditPullRequestsMetadata(): Boolean

  fun isMergeAllowed(): Boolean
  fun isRebaseMergeAllowed(): Boolean
  fun isSquashMergeAllowed(): Boolean

  fun isMergeForbiddenForProject(): Boolean
  fun isUserInAnyTeam(slugs: List<String>): Boolean
}