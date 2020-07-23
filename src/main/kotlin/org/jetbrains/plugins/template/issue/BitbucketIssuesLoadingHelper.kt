// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.issue

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.plugins.template.api.BBRepositoryPath
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutor
import org.jetbrains.plugins.template.api.BitbucketApiRequests
import org.jetbrains.plugins.template.api.BitbucketServerPath
import org.jetbrains.plugins.template.api.data.BitbucketIssue
import org.jetbrains.plugins.template.api.data.BitbucketSearchedIssue
import org.jetbrains.plugins.template.api.util.BitbucketApiPagesLoader


import java.io.IOException

object BitbucketIssuesLoadingHelper {
  @JvmOverloads
  @JvmStatic
  @Throws(IOException::class)
  fun load(executor: BitbucketApiRequestExecutor, indicator: ProgressIndicator, server: BitbucketServerPath,
           owner: String, repo: String, withClosed: Boolean, maximum: Int = 100, assignee: String? = null): List<BitbucketIssue> {
    return BitbucketApiPagesLoader.load(executor, indicator,
            BitbucketApiRequests.Repos.Issues.pages(server, owner, repo,
                                                                           if (withClosed) "all" else "open", assignee), maximum)
  }

  @JvmOverloads
  @JvmStatic
  @Throws(IOException::class)
  fun search(executor: BitbucketApiRequestExecutor, indicator: ProgressIndicator, server: BitbucketServerPath,
             owner: String, repo: String, withClosed: Boolean, assignee: String? = null, query: String? = null)
    : List<BitbucketSearchedIssue> {

    return BitbucketApiPagesLoader.loadAll(executor, indicator,
            BitbucketApiRequests.Search.Issues.pages(server,
                                                                              BBRepositoryPath(owner,
                                                                                               repo),
                                                                              if (withClosed) null else "open", assignee, query))
  }
}