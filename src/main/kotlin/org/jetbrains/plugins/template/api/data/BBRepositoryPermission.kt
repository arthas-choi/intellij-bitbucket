// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data

import org.jetbrains.plugins.template.api.BBRepositoryPath

class BBRepositoryPermission(id: String,
                             val owner: BBRepositoryOwnerName,
                             nameWithOwner: String,
                             val viewerPermission: BBRepositoryPermissionLevel?,
                             val mergeCommitAllowed: Boolean,
                             val squashMergeAllowed: Boolean,
                             val rebaseMergeAllowed: Boolean)
  : BBNode(id) {
  val path: BBRepositoryPath

  init {
    val split = nameWithOwner.split('/')
    path = BBRepositoryPath(split[0], split[1])
  }
}