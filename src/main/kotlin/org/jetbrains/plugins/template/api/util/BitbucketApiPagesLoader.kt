// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.util

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.plugins.template.api.BitbucketApiRequest
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutor
import org.jetbrains.plugins.template.api.data.BitbucketResponsePage
import java.io.IOException
import java.util.function.Predicate

object BitbucketApiPagesLoader {

  @Throws(IOException::class)
  @JvmStatic
  fun <T> loadAll(executor: BitbucketApiRequestExecutor, indicator: ProgressIndicator, pagesRequest: Request<T>): List<T> {
    val result = mutableListOf<T>()
    loadAll(executor, indicator, pagesRequest) { result.addAll(it) }
    return result
  }

  @Throws(IOException::class)
  @JvmStatic
  fun <T> loadAll(executor: BitbucketApiRequestExecutor,
                  indicator: ProgressIndicator,
                  pagesRequest: Request<T>,
                  pageItemsConsumer: (List<T>) -> Unit) {
    var request: BitbucketApiRequest<BitbucketResponsePage<T>>? = pagesRequest.initialRequest
    while (request != null) {
      val page = executor.execute(indicator, request)
      pageItemsConsumer(page.items)
      request = page.nextLink?.let(pagesRequest.urlRequestProvider)
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun <T> find(executor: BitbucketApiRequestExecutor, indicator: ProgressIndicator, pagesRequest: Request<T>, predicate: Predicate<T>): T? {
    var request: BitbucketApiRequest<BitbucketResponsePage<T>>? = pagesRequest.initialRequest
    while (request != null) {
      val page = executor.execute(indicator, request)
      page.items.find { predicate.test(it) }?.let { return it }
      request = page.nextLink?.let(pagesRequest.urlRequestProvider)
    }
    return null
  }

  @Throws(IOException::class)
  @JvmStatic
  fun <T> load(executor: BitbucketApiRequestExecutor, indicator: ProgressIndicator, pagesRequest: Request<T>, maximum: Int): List<T> {
    val result = mutableListOf<T>()
    var request: BitbucketApiRequest<BitbucketResponsePage<T>>? = pagesRequest.initialRequest
    while (request != null) {
      val page = executor.execute(indicator, request)
      for (item in page.items) {
        result.add(item)
        if (result.size == maximum) return result
      }
      request = page.nextLink?.let(pagesRequest.urlRequestProvider)
    }
    return result
  }

  class Request<T>(val initialRequest: BitbucketApiRequest<BitbucketResponsePage<T>>,
                   val urlRequestProvider: (String) -> BitbucketApiRequest<BitbucketResponsePage<T>>)
}