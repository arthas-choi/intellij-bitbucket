// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.util

import com.intellij.util.io.URLUtil
import org.jetbrains.plugins.template.api.data.request.BitbucketRequestPagination


@DslMarker
private annotation class UrlQueryDsl

@UrlQueryDsl
class BitbucketApiUrlQueryBuilder {
  private val builder = StringBuilder()

  fun param(name: String, value: String?) {
    if (value != null) append("$name=${URLUtil.encodeURIComponent(value)}")
  }

  fun param(pagination: BitbucketRequestPagination?) {
    if (pagination != null) {
      param("page", pagination.pageNumber.toString())
      param("per_page", pagination.pageSize.toString())
    }
  }

  private fun append(part: String) {
    if (builder.isEmpty()) builder.append("?") else builder.append("&")
    builder.append(part)
  }

  companion object {
    @JvmStatic
    fun urlQuery(init: BitbucketApiUrlQueryBuilder.() -> Unit) : String {
      val query = BitbucketApiUrlQueryBuilder()
      init(query)
      return query.builder.toString()
    }
  }
}