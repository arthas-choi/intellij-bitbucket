// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest.timeline

import org.jetbrains.plugins.template.api.data.BBActor
import org.jetbrains.plugins.template.api.data.pullrequest.BBPullRequestState
import java.util.*

interface BBPRTimelineEvent : BBPRTimelineItem {
  val actor: BBActor?
  val createdAt: Date

  /**
   * Simple events which can be merged together
   */
  interface Simple : BBPRTimelineEvent

  /**
   * Events about pull request state
   */
  interface State : BBPRTimelineEvent {
    val newState: BBPullRequestState
  }

  /**
   * More complex events which can NOT be merged together
   */
  interface Complex : BBPRTimelineEvent

  /**
   * Pull request head/base branch changes events
   */
  interface Branch : Complex
}