// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.ui.details.action

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project




import java.util.concurrent.CompletableFuture

internal class GHPRCommitMergeAction(busyStateModel: SingleValueModel<Boolean>,
                                     errorHandler: (String) -> Unit,
                                     private val detailsModel: SingleValueModel<GHPullRequestShort>,
                                     mergeabilityModel: SingleValueModel<GHPRMergeabilityState?>,
                                     private val project: Project,
                                     private val stateService: GHPRStateService)
  : GHPRMergeAction("Merge...", busyStateModel, errorHandler, mergeabilityModel) {

  init {
    update()
  }

  override fun submitMergeTask(mergeability: GHPRMergeabilityState): CompletableFuture<Unit>? {
    val dialog = GithubMergeCommitMessageDialog(project,
                                                "Merge Pull Request",
                                                "Merge pull request #${mergeability.number}",
                                                detailsModel.value.title)
    if (!dialog.showAndGet()) {
      return null
    }

    return stateService.merge(EmptyProgressIndicator(), mergeability.number, dialog.message, mergeability.headRefOid)
  }
}