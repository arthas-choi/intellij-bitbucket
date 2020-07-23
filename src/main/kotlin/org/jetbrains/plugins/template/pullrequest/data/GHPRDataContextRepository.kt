// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.data

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionListModel
import com.intellij.util.messages.ListenerDescriptor
import com.intellij.util.messages.MessageBusFactory
import com.intellij.util.messages.MessageBusOwner
import git4idea.commands.Git
import org.jetbrains.annotations.CalledInBackground






import java.io.IOException

@Service
internal class GHPRDataContextRepository(private val project: Project) {
  @CalledInBackground
  @Throws(IOException::class)
  fun getContext(indicator: ProgressIndicator,
                 account: GithubAccount, requestExecutor: GithubApiRequestExecutor,
                 gitRemoteCoordinates: GitRemoteUrlCoordinates): GHPRDataContext {
    val fullPath = GithubUrlUtil.getUserAndRepositoryFromRemoteUrl(gitRemoteCoordinates.url)
                   ?: throw IllegalArgumentException(
                     "Invalid GitHub Repository URL - ${gitRemoteCoordinates.url} is not a GitHub repository")

    indicator.text = "Loading account information"
    val accountDetails = GithubAccountInformationProvider.getInstance().getInformation(requestExecutor, indicator, account)
    indicator.checkCanceled()

    indicator.text = "Loading repository information"
    val repoWithPermissions =
      requestExecutor.execute(indicator, GHGQLRequests.Repo.findPermission(GHRepositoryCoordinates(account.server, fullPath)))
      ?: throw IllegalArgumentException("Repository $fullPath does not exist at ${account.server} or you don't have access.")

    val currentUser = GHUser(accountDetails.nodeId, accountDetails.login, accountDetails.htmlUrl, accountDetails.avatarUrl!!,
                             accountDetails.name)

    indicator.text = "Loading user teams information"
    val repoOwner = repoWithPermissions.owner
    val currentUserTeams = if (repoOwner is GHRepositoryOwnerName.Organization)
      SimpleGHGQLPagesLoader(requestExecutor, {
        GHGQLRequests.Organization.Team.findByUserLogins(account.server, repoOwner.login, listOf(currentUser.login), it)
      }).loadAll(indicator)
    else emptyList()

    val repositoryCoordinates = GHRepositoryCoordinates(account.server, repoWithPermissions.path)

    val messageBus = MessageBusFactory.getInstance().createMessageBus(object : MessageBusOwner {
      override fun isDisposed() = project.isDisposed

      override fun createListener(descriptor: ListenerDescriptor) = throw UnsupportedOperationException()
    })

    val securityService = GHPRSecurityServiceImpl(GithubSharedProjectSettings.getInstance(project), currentUser, currentUserTeams,
                                                  repoWithPermissions)
    val reviewService = GHPRReviewServiceImpl(ProgressManager.getInstance(), messageBus, securityService, requestExecutor,
                                              repositoryCoordinates)
    val commentService = GHPRCommentServiceImpl(ProgressManager.getInstance(), messageBus, securityService, requestExecutor,
                                                repositoryCoordinates)

    val listModel = CollectionListModel<GHPullRequestShort>()
    val searchHolder = GithubPullRequestSearchQueryHolderImpl()
    val listLoader = GHPRListLoaderImpl(ProgressManager.getInstance(), requestExecutor, account.server, repoWithPermissions.path, listModel,
                                        searchHolder)

    val dataLoader = GHPRDataLoaderImpl {
      GHPRDataProviderImpl(project, ProgressManager.getInstance(), Git.getInstance(), securityService, requestExecutor,
                           gitRemoteCoordinates, repositoryCoordinates, it)
    }
    requestExecutor.addListener(dataLoader) {
      dataLoader.invalidateAllData()
    }
    messageBus.connect().subscribe(PULL_REQUEST_EDITED_TOPIC, object : PullRequestEditedListener {
      override fun onPullRequestEdited(number: Long) {
        runInEdt {
          val dataProvider = dataLoader.findDataProvider(number)
          dataProvider?.reloadDetails()
          dataProvider?.detailsRequest?.let { listLoader.reloadData(it) }
          dataProvider?.timelineLoader?.loadMore(true)
        }
      }

      override fun onPullRequestReviewsEdited(number: Long) {
        runInEdt {
          val dataProvider = dataLoader.findDataProvider(number)
          dataProvider?.reloadReviewThreads()
          dataProvider?.timelineLoader?.loadMore(true)
        }
      }

      override fun onPullRequestCommentsEdited(number: Long) {
        runInEdt {
          val dataProvider = dataLoader.findDataProvider(number)
          dataProvider?.timelineLoader?.loadMore(true)
        }
      }
    })
    val metadataService = GHPRMetadataServiceImpl(ProgressManager.getInstance(), messageBus, requestExecutor, account.server,
                                                  repoWithPermissions.path, repoOwner)
    val stateService = GHPRStateServiceImpl(ProgressManager.getInstance(), messageBus,
                                            requestExecutor, account.server, repoWithPermissions.path)

    return GHPRDataContext(gitRemoteCoordinates, repositoryCoordinates, account,
                           requestExecutor, messageBus, listModel, searchHolder, listLoader, dataLoader, securityService,
                           metadataService, stateService, reviewService, commentService)
  }

  companion object {
    fun getInstance(project: Project) = project.service<GHPRDataContextRepository>()
  }
}