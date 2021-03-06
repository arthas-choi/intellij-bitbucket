// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.comment.viewer

import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.diff.util.LineRange
import com.intellij.diff.util.Range
import com.intellij.openapi.editor.impl.EditorImpl





class GHPRSimpleOnesideDiffViewerReviewThreadsHandler(commentableRangesModel: SingleValueModel<List<Range>?>,
                                                      reviewThreadsModel: SingleValueModel<List<GHPRDiffReviewThreadMapping>?>,
                                                      viewer: SimpleOnesideDiffViewer,
                                                      componentsFactory: GHPRDiffEditorReviewComponentsFactory)
  : GHPRDiffViewerBaseReviewThreadsHandler<SimpleOnesideDiffViewer>(commentableRangesModel, reviewThreadsModel, viewer) {

  private val commentableRanges = SingleValueModel<List<LineRange>>(emptyList())
  private val editorThreads = GHPREditorReviewThreadsModel()

  override val viewerReady: Boolean = true

  init {
    val inlaysManager = EditorComponentInlaysManager(viewer.editor as EditorImpl)

    GHPREditorCommentableRangesController(commentableRanges, componentsFactory, inlaysManager) {
      viewer.side to it
    }
    GHPREditorReviewThreadsController(editorThreads, componentsFactory, inlaysManager)
  }

  override fun markCommentableRanges(ranges: List<Range>?) {
    commentableRanges.value = ranges?.let { GHPRCommentsUtil.getLineRanges(it, viewer.side) }.orEmpty()
  }

  override fun showThreads(threads: List<GHPRDiffReviewThreadMapping>?) {
    editorThreads.update(threads
                           ?.filter { it.diffSide == viewer.side }
                           ?.groupBy({ it.fileLineIndex }, { it.thread }).orEmpty())
  }
}
