// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.graphql


import org.jetbrains.plugins.template.api.data.request.BitbucketRequestPagination
import java.util.*

class BBGQLRequestPagination private constructor(val afterCursor: String? = null,
                                                 val since: Date? = null,
                                                 val pageSize: Int = BitbucketRequestPagination.DEFAULT_PAGE_SIZE) {

  constructor(afterCursor: String? = null,
              pageSize: Int = BitbucketRequestPagination.DEFAULT_PAGE_SIZE) : this(afterCursor, null, pageSize)

  constructor(since: Date? = null,
              pageSize: Int = BitbucketRequestPagination.DEFAULT_PAGE_SIZE) : this(null, since, pageSize)


  override fun toString(): String {
    return "afterCursor=$afterCursor&since=$since&per_page=$pageSize"
  }
}
