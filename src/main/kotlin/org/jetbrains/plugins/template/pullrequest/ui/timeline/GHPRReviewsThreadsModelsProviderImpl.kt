// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.ui.timeline

import com.intellij.openapi.Disposable



class GHPRReviewsThreadsModelsProviderImpl(private val dataProvider: GHPRDataProvider,
                                           private val parentDisposable: Disposable)
  : GHPRReviewsThreadsModelsProvider {

  private var threadsByReview = mapOf<String, List<GHPullRequestReviewThread>>()
  private var loading = false
  private val threadsModelsByReview = mutableMapOf<String, GHPRReviewThreadsModel>()
  private var threadsUpdateRequired = false

  init {
    dataProvider.addRequestsChangesListener(parentDisposable, object : GHPRDataProvider.RequestsChangedListener {
      override fun reviewThreadsRequestChanged() {
        if (threadsModelsByReview.isNotEmpty()) requestUpdateReviewsThreads()
      }
    })
  }

  override fun getReviewThreadsModel(reviewId: String): GHPRReviewThreadsModel {
    return threadsModelsByReview.getOrPut(reviewId) {
      val loadedThreads = threadsByReview[reviewId]
      threadsUpdateRequired = true
      if (loadedThreads == null && !loading) requestUpdateReviewsThreads()
      GHPRReviewThreadsModel().apply {
        update(loadedThreads.orEmpty())
      }
    }
  }

  private fun updateReviewsThreads(threads: List<GHPullRequestReviewThread>) {
    threadsByReview = threads.groupBy { it.reviewId }
    for ((reviewId, model) in threadsModelsByReview) {
      model.update(threadsByReview[reviewId].orEmpty())
    }
  }

  private fun requestUpdateReviewsThreads() {
    loading = true
    threadsUpdateRequired = false
    dataProvider.reviewThreadsRequest.handleOnEdt(parentDisposable) { threads, _ ->
      if (threads != null) {
        updateReviewsThreads(threads)
        loading = false
        if (threadsUpdateRequired) requestUpdateReviewsThreads()
      }
      else {
        loading = false
      }
    }
  }
}