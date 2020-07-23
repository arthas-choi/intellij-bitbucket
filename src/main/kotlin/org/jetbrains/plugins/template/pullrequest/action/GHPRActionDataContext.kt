// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.action









interface GHPRActionDataContext {
  val account: GithubAccount

  val securityService: GHPRSecurityService
  val stateService: GHPRStateService
  val reviewService: GHPRReviewService
  val commentService: GHPRCommentService

  val requestExecutor: GithubApiRequestExecutor

  val gitRepositoryCoordinates: GitRemoteUrlCoordinates
  val repositoryCoordinates: GHRepositoryCoordinates

  val avatarIconsProviderFactory: CachingGithubAvatarIconsProvider.Factory
  val currentUser: GHUser

  val pullRequest: Long?
  val pullRequestDetails: GHPullRequestShort?
  val pullRequestDataProvider: GHPRDataProvider?

  fun resetAllData()
}