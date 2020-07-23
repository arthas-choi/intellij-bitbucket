// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.plugins.template.api.data.BBActor
import org.jetbrains.plugins.template.api.data.BBLabel
import org.jetbrains.plugins.template.api.data.BBNodes
import org.jetbrains.plugins.template.api.data.BBUser
import java.util.*

class BBPullRequest(id: String,
                    url: String,
                    number: Long,
                    title: String,
                    state: BBPullRequestState,
                    author: BBActor?,
                    createdAt: Date,
                    @JsonProperty("assignees") assignees: BBNodes<BBUser>,
                    @JsonProperty("labels") labels: BBNodes<BBLabel>,
                    viewerDidAuthor: Boolean,
                    val bodyHTML: String,
                    @JsonProperty("reviewRequests") reviewRequests: BBNodes<BBPullRequestReviewRequest>,
                    val baseRefName: String,
                    val baseRefOid: String,
                    headRefName: String,
                    val headRefOid: String,
                    headRepository: Repository?)
  : BBPullRequestShort(id, url, number, title, state, author, createdAt, assignees, labels, viewerDidAuthor) {

  @JsonIgnore
  val reviewRequests = reviewRequests.nodes

  @JsonIgnore
  val headLabel = headRepository?.nameWithOwner.orEmpty() + ":" + headRefName

  class Repository(val nameWithOwner: String)
}
