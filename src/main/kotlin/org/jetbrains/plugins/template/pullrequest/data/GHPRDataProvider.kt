// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.data

import com.intellij.openapi.Disposable
import org.jetbrains.annotations.CalledInAwt
import java.util.*
import java.util.concurrent.CompletableFuture

interface GHPRDataProvider : GHPRTimelineLoaderHolder {
  val number: Long

  val detailsRequest: CompletableFuture<GHPullRequest>
  val mergeabilityStateRequest: CompletableFuture<GHPRMergeabilityState>
  val headBranchFetchRequest: CompletableFuture<Unit>
  val apiCommitsRequest: CompletableFuture<List<GHCommit>>
  val changesProviderRequest: CompletableFuture<out GHPRChangesProvider>
  val reviewThreadsRequest: CompletableFuture<List<GHPullRequestReviewThread>>

  fun addRequestsChangesListener(listener: RequestsChangedListener)
  fun addRequestsChangesListener(disposable: Disposable, listener: RequestsChangedListener)
  fun removeRequestsChangesListener(listener: RequestsChangedListener)

  @CalledInAwt
  fun reloadDetails()

  @CalledInAwt
  fun reloadChanges()

  @CalledInAwt
  fun reloadReviewThreads()

  @CalledInAwt
  fun reloadMergeabilityState()

  interface RequestsChangedListener : EventListener {
    fun detailsRequestChanged() {}
    fun commitsRequestChanged() {}
    fun reviewThreadsRequestChanged() {}
    fun mergeabilityStateRequestChanged() {}
  }
}