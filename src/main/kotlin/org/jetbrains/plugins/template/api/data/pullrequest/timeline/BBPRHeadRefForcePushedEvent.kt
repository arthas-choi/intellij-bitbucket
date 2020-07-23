// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest.timeline

import org.jetbrains.plugins.template.api.data.BBActor
import org.jetbrains.plugins.template.api.data.BBCommitHash
import org.jetbrains.plugins.template.api.data.pullrequest.BBGitRefName
import java.util.*

class BBPRHeadRefForcePushedEvent(override val actor: BBActor?,
                                  override val createdAt: Date,
                                  val ref: BBGitRefName?,
                                  val beforeCommit: BBCommitHash,
                                  val afterCommit: BBCommitHash)
  : BBPRTimelineEvent.Branch
