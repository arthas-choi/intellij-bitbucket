// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.util

import com.intellij.openapi.actionSystem.ex.ToolbarLabelAction

class BBToolbarLabelAction(text: String) : ToolbarLabelAction() {
  init {
    templatePresentation.text = text
  }
}