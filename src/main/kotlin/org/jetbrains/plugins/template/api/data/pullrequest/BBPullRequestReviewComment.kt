// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.plugins.template.api.data.BBActor
import org.jetbrains.plugins.template.api.data.BBComment
import org.jetbrains.plugins.template.api.data.BBCommitHash
import org.jetbrains.plugins.template.api.data.BBNode
import java.util.*

class BBPullRequestReviewComment(id: String,
                                 val databaseId: Long,
                                 val url: String,
                                 author: BBActor?,
                                 bodyHTML: String,
                                 createdAt: Date,
                                 val path: String,
                                 val commit: BBCommitHash?,
                                 val position: Int?,
                                 val originalCommit: BBCommitHash?,
                                 val originalPosition: Int,
                                 val replyTo: BBNode?,
                                 val diffHunk: String,
                                 @JsonProperty("pullRequestReview") pullRequestReview: BBNode,
                                 val viewerCanDelete: Boolean,
                                 val viewerCanUpdate: Boolean)
  : BBComment(id, author, bodyHTML, createdAt) {
  val reviewId = pullRequestReview.id
}
