// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data


import org.jetbrains.plugins.template.api.data.pullrequest.timeline.BBPRTimelineItem
import java.util.*

open class BBIssueComment(id: String,
                          val url: String,
                          author: BBActor?,
                          bodyHTML: String,
                          createdAt: Date)
  : BBComment(id, author, bodyHTML, createdAt), BBPRTimelineItem
