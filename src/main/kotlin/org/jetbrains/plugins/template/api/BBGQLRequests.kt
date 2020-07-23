// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api

import org.jetbrains.plugins.template.api.data.graphql.BBGQLPagedRequestResponse
import org.jetbrains.plugins.template.api.data.graphql.BBGQLRequestPagination


object BBGQLRequests {
  object Organization {

    object Team {
      fun findAll(server: BitbucketServerPath, organization: String,
                  pagination: BBGQLRequestPagination? = null): BitbucketApiRequest.Post.GQLQuery<BBGQLPagedRequestResponse<BBTeam>> {

        return GQLQuery.TraversedParsed(server.toGraphQLUrl(), BBGQLQueries.findOrganizationTeams,
                                        mapOf("organization" to organization,
                                              "pageSize" to pagination?.pageSize,
                                              "cursor" to pagination?.afterCursor),
                                        TeamsConnection::class.java,
                                        "organization", "teams")
      }

      fun findByUserLogins(server: BitbucketServerPath, organization: String, logins: List<String>,
                           pagination: GHGQLRequestPagination? = null): GQLQuery<GHGQLPagedRequestResponse<GHTeam>> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), BBGQLQueries.findOrganizationTeams,
                                 mapOf("organization" to organization,
                                       "logins" to logins,
                                       "pageSize" to pagination?.pageSize,
                                       "cursor" to pagination?.afterCursor),
                                 TeamsConnection::class.java,
                                 "organization", "teams")

      private class TeamsConnection(pageInfo: GHGQLPageInfo, nodes: List<GHTeam>)
        : GHConnection<GHTeam>(pageInfo, nodes)
    }
  }

  object Repo {
    fun findPermission(repository: BBRepositoryCoordinates): GQLQuery<GHRepositoryPermission?> {
      return GQLQuery.OptionalTraversedParsed(repository.serverPath.toGraphQLUrl(), BBGQLQueries.findRepositoryPermission,
                                              mapOf("repoOwner" to repository.repositoryPath.owner,
                                                    "repoName" to repository.repositoryPath.repository),
                                              GHRepositoryPermission::class.java,
                                              "repository")
    }
  }

  object PullRequest {
    fun findOne(repository: BBRepositoryCoordinates, number: Long): GQLQuery<GHPullRequest?> {
      return GQLQuery.OptionalTraversedParsed(repository.serverPath.toGraphQLUrl(), BBGQLQueries.findPullRequest,
                                              mapOf("repoOwner" to repository.repositoryPath.owner,
                                                    "repoName" to repository.repositoryPath.repository,
                                                    "number" to number),
                                              GHPullRequest::class.java,
                                              "repository", "pullRequest")
    }

    fun mergeabilityData(repository: BBRepositoryCoordinates, number: Long): GQLQuery<GHPullRequestMergeabilityData?> =
      GQLQuery.OptionalTraversedParsed(repository.serverPath.toGraphQLUrl(), BBGQLQueries.pullRequestMergeabilityData,
                                       mapOf("repoOwner" to repository.repositoryPath.owner,
                                             "repoName" to repository.repositoryPath.repository,
                                             "number" to number),
                                       GHPullRequestMergeabilityData::class.java,
                                       "repository", "pullRequest").apply {
        acceptMimeType = "application/vnd.github.antiope-preview+json,application/vnd.github.merge-info-preview+json"
      }

    fun search(server: BitbucketServerPath, query: String, pagination: GHGQLRequestPagination? = null)
      : GQLQuery<GHGQLSearchQueryResponse<GHPullRequestShort>> {

      return GQLQuery.Parsed(server.toGraphQLUrl(), BBGQLQueries.issueSearch,
                             mapOf("query" to query,
                                   "pageSize" to pagination?.pageSize,
                                   "cursor" to pagination?.afterCursor),
                             PRSearch::class.java)
    }

    private class PRSearch(search: SearchConnection<GHPullRequestShort>)
      : GHGQLSearchQueryResponse<GHPullRequestShort>(search)

    fun reviewThreads(repository: BBRepositoryCoordinates, number: Long,
                      pagination: GHGQLRequestPagination? = null): GQLQuery<GHGQLPagedRequestResponse<GHPullRequestReviewThread>> {
      return GQLQuery.TraversedParsed(repository.serverPath.toGraphQLUrl(), BBGQLQueries.pullRequestReviewThreads,
                                      mapOf("repoOwner" to repository.repositoryPath.owner,
                                            "repoName" to repository.repositoryPath.repository,
                                            "number" to number,
                                            "pageSize" to pagination?.pageSize,
                                            "cursor" to pagination?.afterCursor),
                                      ThreadsConnection::class.java,
                                      "repository", "pullRequest", "reviewThreads")
    }

    private class ThreadsConnection(pageInfo: GHGQLPageInfo, nodes: List<GHPullRequestReviewThread>)
      : GHConnection<GHPullRequestReviewThread>(pageInfo, nodes)

    fun commits(repository: BBRepositoryCoordinates, number: Long,
                pagination: GHGQLRequestPagination? = null): GQLQuery<GHGQLPagedRequestResponse<GHPullRequestCommitShort>> {
      return GQLQuery.TraversedParsed(repository.serverPath.toGraphQLUrl(), BBGQLQueries.pullRequestCommits,
                                      mapOf("repoOwner" to repository.repositoryPath.owner,
                                            "repoName" to repository.repositoryPath.repository,
                                            "number" to number,
                                            "pageSize" to pagination?.pageSize,
                                            "cursor" to pagination?.afterCursor),
                                      CommitsConnection::class.java,
                                      "repository", "pullRequest", "commits")
    }

    private class CommitsConnection(pageInfo: GHGQLPageInfo, nodes: List<GHPullRequestCommitShort>)
      : GHConnection<GHPullRequestCommitShort>(pageInfo, nodes)

    object Timeline {
      fun items(server: BitbucketServerPath, repoOwner: String, repoName: String, number: Long,
                pagination: GHGQLRequestPagination? = null)
        : GQLQuery<GHGQLPagedRequestResponse<GHPRTimelineItem>> {

        return GQLQuery.TraversedParsed(server.toGraphQLUrl(), BBGQLQueries.pullRequestTimeline,
                                        mapOf("repoOwner" to repoOwner,
                                              "repoName" to repoName,
                                              "number" to number,
                                              "pageSize" to pagination?.pageSize,
                                              "cursor" to pagination?.afterCursor,
                                              "since" to pagination?.since),
                                        TimelineConnection::class.java,
                                        "repository", "pullRequest", "timelineItems")
      }

      private class TimelineConnection(pageInfo: GHGQLPageInfo, nodes: List<GHPRTimelineItem>)
        : GHConnection<GHPRTimelineItem>(pageInfo, nodes)
    }

    object Review {

      fun getCommentBody(server: BitbucketServerPath, commentId: String): GQLQuery<String> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), BBGQLQueries.getReviewCommentBody,
                                 mapOf("id" to commentId),
                                 String::class.java,
                                 "node", "body")

      fun deleteComment(server: BitbucketServerPath, commentId: String): GQLQuery<Any> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), BBGQLQueries.deleteReviewComment,
                                 mapOf("id" to commentId), Any::class.java)

      fun updateComment(server: BitbucketServerPath, commentId: String, newText: String): GQLQuery<GHPullRequestReviewComment> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), BBGQLQueries.updateReviewComment,
                                 mapOf("id" to commentId,
                                       "body" to newText),
                                 GHPullRequestReviewComment::class.java,
                                 "updatePullRequestReviewComment", "pullRequestReviewComment")
    }
  }
}