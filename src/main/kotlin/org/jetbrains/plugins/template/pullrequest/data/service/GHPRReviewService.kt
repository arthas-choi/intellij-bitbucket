// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.data.service

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInAny

import java.util.concurrent.CompletableFuture

interface GHPRReviewService {

  fun canComment(): Boolean

  @CalledInAny
  fun getCommentMarkdownBody(progressIndicator: ProgressIndicator, commentId: String): CompletableFuture<String>

  @CalledInAny
  fun addComment(progressIndicator: ProgressIndicator, pullRequest: Long, body: String, replyToCommentId: Long)
    : CompletableFuture<GithubPullRequestCommentWithHtml>

  @CalledInAny
  fun addComment(progressIndicator: ProgressIndicator,
                 pullRequest: Long,
                 body: String,
                 commitSha: String,
                 fileName: String,
                 diffLine: Int): CompletableFuture<GithubPullRequestCommentWithHtml>

  @CalledInAny
  fun deleteComment(progressIndicator: ProgressIndicator, pullRequest: Long, commentId: String): CompletableFuture<Unit>

  @CalledInAny
  fun updateComment(progressIndicator: ProgressIndicator, pullRequest: Long, commentId: String, newText: String)
    : CompletableFuture<GHPullRequestReviewComment>
}
