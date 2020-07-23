// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.data.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInAwt
import org.jetbrains.annotations.CalledInBackground
import java.util.concurrent.CompletableFuture

interface GHPRMetadataService : Disposable {
  val collaboratorsWithPushAccess: CompletableFuture<List<GHUser>>
  val teams: CompletableFuture<List<GHTeam>>
  val potentialReviewers: CompletableFuture<List<GHPullRequestRequestedReviewer>>
  val issuesAssignees: CompletableFuture<List<GHUser>>
  val labels: CompletableFuture<List<GHLabel>>

  @CalledInAwt
  fun resetData()

  @CalledInBackground
  fun adjustReviewers(indicator: ProgressIndicator, pullRequest: Long, delta: CollectionDelta<GHPullRequestRequestedReviewer>)

  @CalledInBackground
  fun adjustAssignees(indicator: ProgressIndicator, pullRequest: Long, delta: CollectionDelta<GHUser>)

  @CalledInBackground
  fun adjustLabels(indicator: ProgressIndicator, pullRequest: Long, delta: CollectionDelta<GHLabel>)
}