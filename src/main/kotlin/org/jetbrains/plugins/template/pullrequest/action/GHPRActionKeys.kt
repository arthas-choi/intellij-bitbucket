// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.action

import com.intellij.openapi.actionSystem.DataKey

object GHPRActionKeys {
  @JvmStatic
  val ACTION_DATA_CONTEXT = DataKey.create<GHPRActionDataContext>("org.jetbrains.plugins.github.pullrequest.datacontext")

  @JvmStatic
  internal val SELECTED_PULL_REQUEST = DataKey.create<GHPullRequestShort>("org.jetbrains.plugins.github.pullrequest.list.selected")
}