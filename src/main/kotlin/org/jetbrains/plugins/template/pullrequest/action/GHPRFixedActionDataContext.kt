// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.action



class GHPRFixedActionDataContext internal constructor(private val delegate: GHPRActionDataContext,
                                                      dataProvider: GHPRDataProvider,
                                                      details: GHPullRequestShort? = null)
  : GHPRActionDataContext {

  override val account = delegate.account

  override val securityService = delegate.securityService
  override val stateService = delegate.stateService
  override val reviewService = delegate.reviewService
  override val commentService = delegate.commentService

  override val requestExecutor = delegate.requestExecutor

  override val gitRepositoryCoordinates = delegate.gitRepositoryCoordinates
  override val repositoryCoordinates = delegate.repositoryCoordinates

  override val avatarIconsProviderFactory = delegate.avatarIconsProviderFactory
  override val currentUser = delegate.securityService.currentUser

  override val pullRequest = dataProvider.number
  override val pullRequestDetails = details
  override val pullRequestDataProvider = dataProvider

  override fun resetAllData() = delegate.resetAllData()
}