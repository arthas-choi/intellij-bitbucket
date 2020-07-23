// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest

import org.jetbrains.plugins.template.api.data.BBNode

class BBTeam(id: String,
             val slug: String,
             override val url: String,
             override val avatarUrl: String,
             override val name: String?,
             val combinedSlug: String)
  : BBNode(id), BBPullRequestRequestedReviewer {
  override val shortName: String = combinedSlug
}