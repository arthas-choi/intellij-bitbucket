// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.ui.details.action

import com.intellij.openapi.progress.EmptyProgressIndicator



internal class GHPRCloseAction(busyStateModel: SingleValueModel<Boolean>,
                               errorHandler: (String) -> Unit,
                               private val stateService: GHPRStateService,
                               private val number: Long)
  : GHPRStateChangeAction("Close", busyStateModel, errorHandler) {

  init {
    update()
  }

  override val errorPrefix = "Error occurred while closing pull request:"

  override fun submitTask() = stateService.close(EmptyProgressIndicator(), number)
}