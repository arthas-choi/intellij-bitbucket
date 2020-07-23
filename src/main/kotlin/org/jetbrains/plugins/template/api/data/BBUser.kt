// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data

import org.jetbrains.plugins.template.api.data.pullrequest.BBPullRequestRequestedReviewer

class BBUser(id: String,
             override val login: String,
             override val url: String,
             override val avatarUrl: String,
             override val name: String?)
  : BBNode(id), BBActor, BBPullRequestRequestedReviewer {
  override val shortName: String = login
}
