// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data

import com.google.common.annotations.VisibleForTesting

class BBLabel(id: String,
              val url: String,
              val name: String,
              val color: String)
  : BBNode(id) {

  companion object {
    @VisibleForTesting
    internal fun createTest(id: String) = BBLabel(id, "", "testLabel_$id", "#FFFFFF")
  }
}