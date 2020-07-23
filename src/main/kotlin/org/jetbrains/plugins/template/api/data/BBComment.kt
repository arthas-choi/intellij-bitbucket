// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data

import java.util.*

open class BBComment(id: String,
                     val author: BBActor?,
                     val bodyHtml: String,
                     val createdAt: Date)
  : BBNode(id)
