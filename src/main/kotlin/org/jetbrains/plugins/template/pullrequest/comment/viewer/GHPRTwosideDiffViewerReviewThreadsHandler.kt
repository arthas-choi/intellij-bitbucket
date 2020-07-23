// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.comment.viewer

import com.intellij.diff.tools.util.side.TwosideTextDiffViewer
import com.intellij.diff.util.LineRange
import com.intellij.diff.util.Range
import com.intellij.diff.util.Side
import com.intellij.openapi.editor.impl.EditorImpl





class GHPRTwosideDiffViewerReviewThreadsHandler(commentableRangesModel: SingleValueModel<List<Range>?>,
                                                reviewThreadsModel: SingleValueModel<List<GHPRDiffReviewThreadMapping>?>,
                                                viewer: TwosideTextDiffViewer,
                                                componentsFactory: GHPRDiffEditorReviewComponentsFactory)
  : GHPRDiffViewerBaseReviewThreadsHandler<TwosideTextDiffViewer>(commentableRangesModel, reviewThreadsModel, viewer) {

  private val commentableRangesLeft = SingleValueModel<List<LineRange>>(emptyList())
  private val editorThreadsLeft = GHPREditorReviewThreadsModel()

  private val commentableRangesRight = SingleValueModel<List<LineRange>>(emptyList())
  private val editorThreadsRight = GHPREditorReviewThreadsModel()

  override val viewerReady = true

  init {
    val inlaysManagerLeft = EditorComponentInlaysManager(viewer.editor1 as EditorImpl)

    GHPREditorCommentableRangesController(commentableRangesLeft, componentsFactory, inlaysManagerLeft) {
      Side.LEFT to it
    }
    GHPREditorReviewThreadsController(editorThreadsLeft, componentsFactory, inlaysManagerLeft)

    val inlaysManagerRight = EditorComponentInlaysManager(viewer.editor2 as EditorImpl)

    GHPREditorCommentableRangesController(commentableRangesRight, componentsFactory, inlaysManagerRight) {
      Side.RIGHT to it
    }
    GHPREditorReviewThreadsController(editorThreadsRight, componentsFactory, inlaysManagerRight)
  }

  override fun markCommentableRanges(ranges: List<Range>?) {
    commentableRangesLeft.value = ranges?.let { GHPRCommentsUtil.getLineRanges(it, Side.LEFT) }.orEmpty()
    commentableRangesRight.value = ranges?.let { GHPRCommentsUtil.getLineRanges(it, Side.RIGHT) }.orEmpty()
  }

  override fun showThreads(threads: List<GHPRDiffReviewThreadMapping>?) {
    editorThreadsLeft.update(threads
                               ?.filter { it.diffSide == Side.LEFT }
                               ?.groupBy({ it.fileLineIndex }, { it.thread }).orEmpty())
    editorThreadsRight.update(threads
                                ?.filter { it.diffSide == Side.RIGHT }
                                ?.groupBy({ it.fileLineIndex }, { it.thread }).orEmpty())
  }
}

