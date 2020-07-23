// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.data

import com.intellij.openapi.vcs.changes.Change

interface GHPRChangesProvider {
  val changes: List<Change>
  val changesByCommits: Map<GHCommit, List<Change>>

  fun findChangeDiffData(change: Change): GHPRChangeDiffData?
}