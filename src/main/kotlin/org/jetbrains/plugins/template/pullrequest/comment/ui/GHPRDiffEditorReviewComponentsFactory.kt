// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.comment.ui

import com.intellij.diff.util.Side
import javax.swing.JComponent

interface GHPRDiffEditorReviewComponentsFactory {
  fun createThreadComponent(thread: GHPRReviewThreadModel): JComponent
  fun createCommentComponent(side: Side, line: Int, hideCallback: () -> Unit): JComponent
}