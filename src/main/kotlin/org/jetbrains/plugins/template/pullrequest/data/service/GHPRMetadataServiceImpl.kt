// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.data.service

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.messages.MessageBus
import org.jetbrains.annotations.CalledInBackground



import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction

class GHPRMetadataServiceImpl internal constructor(progressManager: ProgressManager,
                                                   private val messageBus: MessageBus,
                                                   private val requestExecutor: BitbucketApiRequestExecutor,
                                                   private val serverPath: GithubServerPath,
                                                   private val repoPath: GHRepositoryPath,
                                                   private val repoOwner: GHRepositoryOwnerName)
  : GHPRMetadataService {

  init {
    requestExecutor.addListener(this) {
      resetData()
    }
  }

  private val collaboratorsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GithubApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GithubApiRequests.Repos.Collaborators.pages(serverPath, repoPath.owner, repoPath.repository))
      .filter { it.permissions.isPush }
      .map { GHUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) }
  }

  override val collaboratorsWithPushAccess: CompletableFuture<List<GHUser>>
    get() = GithubAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { collaboratorsValue.value } }

  private val teamsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    if (repoOwner !is GHRepositoryOwnerName.Organization) emptyList()
    else SimpleGHGQLPagesLoader(requestExecutor, {
      GHGQLRequests.Organization.Team.findAll(serverPath, repoOwner.login, it)
    }).loadAll(indicator)
  }

  override val teams: CompletableFuture<List<GHTeam>>
    get() = GithubAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { teamsValue.value } }

  override val potentialReviewers: CompletableFuture<List<GHPullRequestRequestedReviewer>>
    get() = GithubAsyncUtil.futureOfMutable {
      invokeAndWaitIfNeeded {
        collaboratorsWithPushAccess.thenCombine(teams,
                                                BiFunction<List<GHUser>, List<GHTeam>, List<GHPullRequestRequestedReviewer>> { users, teams ->
                                                  users + teams
                                                })
      }
    }

  private val assigneesValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GithubApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GithubApiRequests.Repos.Assignees.pages(serverPath, repoPath.owner, repoPath.repository))
      .map { GHUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) }
  }

  override val issuesAssignees: CompletableFuture<List<GHUser>>
    get() = GithubAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { assigneesValue.value } }
  private val labelsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GithubApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GithubApiRequests.Repos.Labels.pages(serverPath, repoPath.owner, repoPath.repository))
      .map { GHLabel(it.nodeId, it.url, it.name, it.color) }
  }

  override val labels: CompletableFuture<List<GHLabel>>
    get() = GithubAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { labelsValue.value } }

  override fun resetData() {
    collaboratorsValue.drop()
    teamsValue.drop()
    assigneesValue.drop()
    labelsValue.drop()
  }

  @CalledInBackground
  override fun adjustReviewers(indicator: ProgressIndicator, pullRequest: Long, delta: CollectionDelta<GHPullRequestRequestedReviewer>) {
    if (delta.isEmpty) return

    val removedItems = delta.removedItems
    if (removedItems.isNotEmpty()) {
      indicator.text2 = "Removing reviewers"
      requestExecutor.execute(indicator,
                              GithubApiRequests.Repos.PullRequests.Reviewers
                                .remove(serverPath, repoPath.owner, repoPath.repository, pullRequest,
                                        removedItems.filterIsInstance(GHUser::class.java).map { it.login },
                                        removedItems.filterIsInstance(GHTeam::class.java).map { it.slug }))
    }
    val newItems = delta.newItems
    if (newItems.isNotEmpty()) {
      indicator.text2 = "Adding reviewers"
      requestExecutor.execute(indicator,
                              GithubApiRequests.Repos.PullRequests.Reviewers
                                .add(serverPath, repoPath.owner, repoPath.repository, pullRequest,
                                     newItems.filterIsInstance(GHUser::class.java).map { it.login },
                                     newItems.filterIsInstance(GHTeam::class.java).map { it.slug }))
    }
    messageBus.syncPublisher(PULL_REQUEST_EDITED_TOPIC).onPullRequestEdited(pullRequest)
  }

  @CalledInBackground
  override fun adjustAssignees(indicator: ProgressIndicator, pullRequest: Long, delta: CollectionDelta<GHUser>) {
    if (delta.isEmpty) return

    requestExecutor.execute(indicator,
                            GithubApiRequests.Repos.Issues.updateAssignees(serverPath, repoPath.owner, repoPath.repository,
                                                                           pullRequest.toString(), delta.newCollection.map { it.login }))
    messageBus.syncPublisher(PULL_REQUEST_EDITED_TOPIC).onPullRequestEdited(pullRequest)
  }

  @CalledInBackground
  override fun adjustLabels(indicator: ProgressIndicator, pullRequest: Long, delta: CollectionDelta<GHLabel>) {
    if (delta.isEmpty) return

    requestExecutor.execute(indicator,
                            GithubApiRequests.Repos.Issues.Labels
                              .replace(serverPath, repoPath.owner, repoPath.repository, pullRequest.toString(),
                                       delta.newCollection.map { it.name }))
    messageBus.syncPublisher(PULL_REQUEST_EDITED_TOPIC).onPullRequestEdited(pullRequest)
  }

  override fun dispose() {
    resetData()
  }
}