// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.data

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager

abstract class GHGQLPagedListLoader<T>(progressManager: ProgressManager,
                                       private val loader: SimpleGHGQLPagesLoader<T>)
  : GHListLoaderBase<T>(progressManager) {

  override fun canLoadMore() = !loading && (loader.hasNext || error != null)

  override fun doLoadMore(indicator: ProgressIndicator, update: Boolean) = loader.loadNext(indicator, update)

  override fun reset() {
    loader.reset()
    super.reset()
  }
}
