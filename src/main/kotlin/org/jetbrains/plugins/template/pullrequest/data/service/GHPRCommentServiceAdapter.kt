// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.data.service

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInAny


import java.util.concurrent.CompletableFuture

interface GHPRCommentServiceAdapter {

  @CalledInAny
  fun canComment(): Boolean

  @CalledInAny
  fun addComment(progressIndicator: ProgressIndicator, body: String)
    : CompletableFuture<GithubIssueCommentWithHtml>

  companion object {
    @CalledInAny
    fun create(commentService: GHPRCommentService, dataProvider: GHPRDataProvider): GHPRCommentServiceAdapter {
      return object : GHPRCommentServiceAdapter {

        override fun canComment() = commentService.canComment()

        override fun addComment(progressIndicator: ProgressIndicator, body: String) =
          commentService.addComment(progressIndicator, dataProvider.number, body)
      }
    }
  }
}