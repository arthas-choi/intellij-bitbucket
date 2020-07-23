// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.comment

import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.diff.tools.util.base.DiffViewerBase
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer
import com.intellij.diff.util.Range
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger









import kotlin.properties.Delegates

class GHPRDiffReviewSupportImpl(private val reviewService: GHPRReviewServiceAdapter,
                                private val diffRanges: List<Range>,
                                private val reviewThreadMapper: (GHPullRequestReviewThread) -> GHPRDiffReviewThreadMapping?,
                                private val createCommentParametersHelper: GHPRCreateDiffCommentParametersHelper,
                                private val avatarIconsProviderFactory: CachingGithubAvatarIconsProvider.Factory,
                                private val currentUser: GHUser)
  : GHPRDiffReviewSupport {

  private val reviewThreadsModel = SingleValueModel<List<GHPRDiffReviewThreadMapping>?>(null)

  override var isLoadingReviewThreads: Boolean = false
    private set

  override var showReviewThreads by Delegates.observable(true) { _, _, newValue ->
    if (newValue) reloadReviewThreads()
    else reviewThreadsModel.value = null
  }

  override fun install(viewer: DiffViewerBase) {
    val diffRangesModel = SingleValueModel(if (reviewService.canComment()) diffRanges else null)
    loadReviewThreads(reviewThreadsModel, viewer)

    val componentsFactory = GHPRDiffEditorReviewComponentsFactoryImpl(reviewService,
                                                                      createCommentParametersHelper,
                                                                      avatarIconsProviderFactory, currentUser)
    when (viewer) {
      is SimpleOnesideDiffViewer ->
        GHPRSimpleOnesideDiffViewerReviewThreadsHandler(diffRangesModel, reviewThreadsModel, viewer, componentsFactory)
      is UnifiedDiffViewer ->
        GHPRUnifiedDiffViewerReviewThreadsHandler(diffRangesModel, reviewThreadsModel, viewer, componentsFactory)
      is TwosideTextDiffViewer ->
        GHPRTwosideDiffViewerReviewThreadsHandler(diffRangesModel, reviewThreadsModel, viewer, componentsFactory)
      else -> return
    }
  }

  override fun reloadReviewThreads() {
    reviewService.resetReviewThreads()
  }

  private fun loadReviewThreads(threadsModel: SingleValueModel<List<GHPRDiffReviewThreadMapping>?>, disposable: Disposable) {
    doLoadReviewThreads(threadsModel, disposable)
    reviewService.addReviewThreadsListener(disposable) {
      doLoadReviewThreads(threadsModel, disposable)
    }
  }

  private fun doLoadReviewThreads(threadsModel: SingleValueModel<List<GHPRDiffReviewThreadMapping>?>, disposable: Disposable) {
    isLoadingReviewThreads = true
    reviewService.loadReviewThreads()
      .successAsync(ProcessIOExecutorService.INSTANCE) {
        it.mapNotNull(reviewThreadMapper)
      }
      .handleOnEdt(disposable) { result, error ->
        if (result != null) {
          if (showReviewThreads)
            threadsModel.value = result
        }
        if (error != null) {
          LOG.info("Failed to load review threads", error)
        }
        isLoadingReviewThreads = false
      }
  }

  companion object {
    val LOG = logger<GHPRDiffReviewSupportImpl>()
  }
}