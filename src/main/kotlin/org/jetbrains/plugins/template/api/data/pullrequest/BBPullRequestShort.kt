// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.plugins.template.api.data.*

import java.util.*

open class BBPullRequestShort(id: String,
                              val url: String,
                              val number: Long,
                              val title: String,
                              val state: BBPullRequestState,
                              val author: BBActor?,
                              val createdAt: Date,
                              @JsonProperty("assignees") assignees: BBNodes<BBUser>,
                              @JsonProperty("labels") labels: BBNodes<BBLabel>,
                              val viewerDidAuthor: Boolean) : BBNode(id) {

  @JsonIgnore
  val assignees = assignees.nodes

  @JsonIgnore
  val labels = labels.nodes
}
