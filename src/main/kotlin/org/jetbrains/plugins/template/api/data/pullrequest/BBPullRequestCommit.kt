// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest

import org.jetbrains.plugins.template.api.data.BBCommit
import org.jetbrains.plugins.template.api.data.BBNode
import org.jetbrains.plugins.template.api.data.pullrequest.timeline.BBPRTimelineItem


class BBPullRequestCommit(id: String,
                          val commit: BBCommit,
                          val url: String)
  : BBNode(id), BBPRTimelineItem