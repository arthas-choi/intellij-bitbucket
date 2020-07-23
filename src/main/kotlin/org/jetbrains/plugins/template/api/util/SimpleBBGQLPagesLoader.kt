// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.util

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.plugins.template.api.BitbucketApiRequest
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutor
import org.jetbrains.plugins.template.api.data.graphql.BBGQLPagedRequestResponse
import org.jetbrains.plugins.template.api.data.graphql.BBGQLRequestPagination
import org.jetbrains.plugins.template.api.data.request.BitbucketRequestPagination


class SimpleBBGQLPagesLoader<T>(executor: BitbucketApiRequestExecutor,
                                requestProducer: (BBGQLRequestPagination) -> BitbucketApiRequest.Post<BBGQLPagedRequestResponse<T>>,
                                supportsTimestampUpdates: Boolean = false,
                                pageSize: Int = BitbucketRequestPagination.DEFAULT_PAGE_SIZE)
  : BBGQLPagesLoader<BBGQLPagedRequestResponse<T>, List<T>>(executor, requestProducer, supportsTimestampUpdates, pageSize) {

  fun loadAll(progressIndicator: ProgressIndicator): List<T> {
    val list = mutableListOf<T>()
    while (hasNext) {
      loadNext(progressIndicator)?.let { list.addAll(it) }
    }
    return list
  }

  override fun extractPageInfo(result: BBGQLPagedRequestResponse<T>) = result.pageInfo

  override fun extractResult(result: BBGQLPagedRequestResponse<T>) = result.nodes
}