// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api

object BBGQLQueries {
  const val findOrganizationTeams = "findOrganizationTeams"
  const val findRepositoryPermission = "findRepositoryPermission"
  const val issueSearch = "issueSearch"
  const val findPullRequest = "findPullRequest"
  const val pullRequestTimeline = "pullRequestTimeline"
  const val pullRequestReviewThreads = "pullRequestReviewThreads"
  const val pullRequestCommits = "pullRequestCommits"
  const val pullRequestMergeabilityData = "findPullRequestMergeability"
  const val getReviewCommentBody = "getReviewCommentBody"
  const val deleteReviewComment = "deleteReviewComment"
  const val updateReviewComment = "updateReviewComment"
}