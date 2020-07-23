// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data

class BBCommit(id: String,
               oid: String,
               abbreviatedOid: String,
               val url: String,
               val message: String,
               val messageHeadlineHTML: String,
               val messageBodyHTML: String,
               val author: BBGitActor?)
  : BBCommitHash(id, oid, abbreviatedOid)