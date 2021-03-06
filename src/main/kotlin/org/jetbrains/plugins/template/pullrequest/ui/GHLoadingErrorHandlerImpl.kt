// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.ui

import com.intellij.openapi.project.Project


import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

class GHLoadingErrorHandlerImpl(private val project: Project,
                                private val account: GithubAccount,
                                private val resetRunnable: () -> Unit)
  : GHLoadingErrorHandler {

  override fun getActionForError(error: Throwable): Action? {
    if (error is GithubAuthenticationException) {
      return ReLoginAction()
    }
    else {
      return RetryAction()
    }
  }

  private inner class ReLoginAction : AbstractAction("Re-Login") {
    override fun actionPerformed(e: ActionEvent?) {
      if (GithubAuthenticationManager.getInstance().requestReLogin(account, project))
        resetRunnable()
    }
  }

  private inner class RetryAction : AbstractAction("Retry") {
    override fun actionPerformed(e: ActionEvent?) {
      resetRunnable()
    }
  }
}