// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.action






class GHPRListSelectionActionDataContext internal constructor(private val dataContext: GHPRDataContext,
                                                              private val selectionHolder: GithubPullRequestsListSelectionHolder,
                                                              override val avatarIconsProviderFactory: CachingGithubAvatarIconsProvider.Factory)
  : GHPRActionDataContext {

  override val account = dataContext.account

  override val gitRepositoryCoordinates = dataContext.gitRepositoryCoordinates
  override val repositoryCoordinates = dataContext.repositoryCoordinates

  override val securityService = dataContext.securityService
  override val stateService = dataContext.stateService
  override val reviewService = dataContext.reviewService
  override val commentService = dataContext.commentService

  override val requestExecutor = dataContext.requestExecutor

  override val currentUser = dataContext.securityService.currentUser

  override fun resetAllData() {
    dataContext.metadataService.resetData()
    dataContext.listLoader.reset()
    dataContext.dataLoader.invalidateAllData()
  }

  override val pullRequest: Long?
    get() = selectionHolder.selectionNumber

  override val pullRequestDetails: GHPullRequestShort?
    get() = pullRequest?.let { dataContext.listLoader.findData(it) }

  override val pullRequestDataProvider: GHPRDataProvider?
    get() = pullRequest?.let { dataContext.dataLoader.getDataProvider(it) }
}