// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.comment.ui

import java.util.*
import javax.swing.ListModel

interface GHPRReviewThreadModel : ListModel<GHPRReviewCommentModel> {
  val id: String
  val createdAt: Date
  val filePath: String
  val diffHunk: String
  val firstCommentDatabaseId: Long

  fun update(thread: GHPullRequestReviewThread)
  fun addComment(comment: GHPRReviewCommentModel)
  fun removeComment(comment: GHPRReviewCommentModel)

  fun addDeletionListener(listener: () -> Unit)
}
