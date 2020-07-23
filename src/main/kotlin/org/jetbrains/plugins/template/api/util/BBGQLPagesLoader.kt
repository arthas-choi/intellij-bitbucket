// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.util

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.plugins.template.api.BitbucketApiRequest
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutor
import org.jetbrains.plugins.template.api.data.graphql.BBGQLPageInfo
import org.jetbrains.plugins.template.api.data.graphql.BBGQLRequestPagination
import org.jetbrains.plugins.template.api.data.request.BitbucketRequestPagination


import java.util.*
import java.util.concurrent.atomic.AtomicReference

abstract class BBGQLPagesLoader<T, R>(private val executor: BitbucketApiRequestExecutor,
                                      private val requestProducer: (BBGQLRequestPagination) -> BitbucketApiRequest.Post<T>,
                                      private val supportsTimestampUpdates: Boolean = false,
                                      private val pageSize: Int = BitbucketRequestPagination.DEFAULT_PAGE_SIZE) {

  private val iterationDataRef = AtomicReference(IterationData(true))

  val hasNext: Boolean
    get() = iterationDataRef.get().hasNext

  @Synchronized
  fun loadNext(progressIndicator: ProgressIndicator, update: Boolean = false): R? {
    val iterationData = iterationDataRef.get()

    val pagination: BBGQLRequestPagination =
      if (update) {
        if (hasNext || !supportsTimestampUpdates) return null
        BBGQLRequestPagination(iterationData.timestamp, pageSize)
      }
      else {
        if (!hasNext) return null
        BBGQLRequestPagination(iterationData.cursor, pageSize)
      }

    val executionDate = Date()
    val response = executor.execute(progressIndicator, requestProducer(pagination))
    val page = extractPageInfo(response)
    iterationDataRef.compareAndSet(iterationData, IterationData(page, executionDate))

    return extractResult(response)
  }

  fun reset() {
    iterationDataRef.set(IterationData(true))
  }

  protected abstract fun extractPageInfo(result: T): BBGQLPageInfo
  protected abstract fun extractResult(result: T): R

  private class IterationData(val hasNext: Boolean, val timestamp: Date? = null, val cursor: String? = null) {
    constructor(page: BBGQLPageInfo, timestamp: Date) : this(page.hasNextPage, timestamp, page.endCursor)
  }
}