// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.data.service



class GHPRSecurityServiceImpl(private val sharedProjectSettings: GithubSharedProjectSettings,
                              override val currentUser: GHUser,
                              private val currentUserTeams: List<GHTeam>,
                              private val repo: GHRepositoryPermission) : GHPRSecurityService {
  override fun isCurrentUser(user: GithubUser) = user.nodeId == currentUser.id
  override fun currentUserCanEditPullRequestsMetadata() = currentUserHasPermissionLevel(GHRepositoryPermissionLevel.TRIAGE)

  override fun currentUserHasPermissionLevel(level: GHRepositoryPermissionLevel) =
    (repo.viewerPermission?.ordinal ?: -1) >= level.ordinal

  override fun isUserInAnyTeam(slugs: List<String>) = currentUserTeams.any { slugs.contains(it.slug) }

  override fun isMergeAllowed() = repo.mergeCommitAllowed
  override fun isRebaseMergeAllowed() = repo.rebaseMergeAllowed
  override fun isSquashMergeAllowed() = repo.squashMergeAllowed

  override fun isMergeForbiddenForProject() = sharedProjectSettings.pullRequestMergeForbidden
}