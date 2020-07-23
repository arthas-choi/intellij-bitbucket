// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.data.service

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.messages.MessageBus



import java.util.concurrent.CompletableFuture

class GHPRReviewServiceImpl(private val progressManager: ProgressManager,
                            private val messageBus: MessageBus,
                            private val securityService: GHPRSecurityService,
                            private val requestExecutor: GithubApiRequestExecutor,
                            private val repository: GHRepositoryCoordinates) : GHPRReviewService {

  override fun canComment() = securityService.currentUserHasPermissionLevel(GHRepositoryPermissionLevel.READ)

  override fun getCommentMarkdownBody(progressIndicator: ProgressIndicator, commentId: String): CompletableFuture<String> {
    return progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(GHGQLRequests.PullRequest.Review.getCommentBody(repository.serverPath, commentId))
    }
  }

  override fun addComment(progressIndicator: ProgressIndicator,
                          pullRequest: Long,
                          body: String,
                          replyToCommentId: Long): CompletableFuture<GithubPullRequestCommentWithHtml> {
    return progressManager.submitIOTask(progressIndicator) {
      val comment = requestExecutor.execute(
        GithubApiRequests.Repos.PullRequests.Comments.createReply(repository, pullRequest, replyToCommentId, body))
      messageBus.syncPublisher(GHPRDataContext.PULL_REQUEST_EDITED_TOPIC).onPullRequestReviewsEdited(pullRequest)
      comment
    }
  }

  override fun addComment(progressIndicator: ProgressIndicator,
                          pullRequest: Long,
                          body: String,
                          commitSha: String,
                          fileName: String,
                          diffLine: Int): CompletableFuture<GithubPullRequestCommentWithHtml> {
    return progressManager.submitIOTask(progressIndicator) {
      val comment = requestExecutor.execute(
        GithubApiRequests.Repos.PullRequests.Comments.create(repository, pullRequest, commitSha, fileName, diffLine, body))
      messageBus.syncPublisher(GHPRDataContext.PULL_REQUEST_EDITED_TOPIC).onPullRequestReviewsEdited(pullRequest)
      comment
    }
  }

  override fun deleteComment(progressIndicator: ProgressIndicator, pullRequest: Long, commentId: String) =
    progressManager.submitIOTask(progressIndicator) {
      requestExecutor.execute(GHGQLRequests.PullRequest.Review.deleteComment(repository.serverPath, commentId))
      messageBus.syncPublisher(GHPRDataContext.PULL_REQUEST_EDITED_TOPIC).onPullRequestReviewsEdited(pullRequest)
    }

  override fun updateComment(progressIndicator: ProgressIndicator, pullRequest: Long, commentId: String, newText: String) =
    progressManager.submitIOTask(progressIndicator) {
      val comment = requestExecutor.execute(GHGQLRequests.PullRequest.Review.updateComment(repository.serverPath, commentId, newText))
      messageBus.syncPublisher(GHPRDataContext.PULL_REQUEST_EDITED_TOPIC).onPullRequestReviewsEdited(pullRequest)
      comment
    }
}