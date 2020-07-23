// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api

import com.intellij.openapi.util.io.StreamUtil
import com.intellij.util.ThrowableConvertor
import org.jetbrains.plugins.template.api.BitbucketApiRequest.Post.Companion
import org.jetbrains.plugins.template.api.data.*
import org.jetbrains.plugins.template.api.data.request.*
import org.jetbrains.plugins.template.api.util.BitbucketApiPagesLoader
import org.jetbrains.plugins.template.api.util.BitbucketApiSearchQueryBuilder
import org.jetbrains.plugins.template.api.util.BitbucketApiUrlQueryBuilder


import java.awt.Image

/**
 * Collection of factory methods for API requests used in plugin
 * TODO: improve url building (DSL?)
 */
object BitbucketApiRequests {
  object CurrentUser : Entity("/user") {
    @JvmStatic
    fun get(server: BitbucketServerPath) = get(getUrl(server, urlSuffix))

    @JvmStatic
    fun get(url: String) = BitbucketApiRequest.Get.json<BitbucketAuthenticatedUser>(url).withOperationName("get profile information")

    @JvmStatic
    fun getAvatar(url: String) = object : BitbucketApiRequest.Get<Image>(url) {
      override fun extractResult(response: BitbucketApiResponse): Image {
        return response.handleBody(ThrowableConvertor {
          BitbucketApiContentHelper.loadImage(it)
        })
      }
    }.withOperationName("get profile avatar")

    object Repos : Entity("/repos") {
      @JvmOverloads
      @JvmStatic
      fun pages(server: BitbucketServerPath,
                type: Type = Type.DEFAULT,
                visibility: Visibility = Visibility.DEFAULT,
                affiliation: Affiliation = Affiliation.DEFAULT,
                pagination: BitbucketRequestPagination? = null) =
              BitbucketApiPagesLoader.Request(get(server, type, visibility, affiliation, pagination), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: BitbucketServerPath,
              type: Type = Type.DEFAULT,
              visibility: Visibility = Visibility.DEFAULT,
              affiliation: Affiliation = Affiliation.DEFAULT,
              pagination: BitbucketRequestPagination? = null): BitbucketApiRequest<BitbucketResponsePage<BitbucketRepo>> {
        if (type != Type.DEFAULT && (visibility != Visibility.DEFAULT || affiliation != Affiliation.DEFAULT)) {
          throw IllegalArgumentException("Param 'type' should not be used together with 'visibility' or 'affiliation'")
        }

        return get(getUrl(server, CurrentUser.urlSuffix, urlSuffix,
                          getQuery(type.toString(), visibility.toString(), affiliation.toString(), pagination?.toString().orEmpty())))
      }

      @JvmStatic
      fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketRepo>(url).withOperationName("get user repositories")

      @JvmStatic
      fun create(server: BitbucketServerPath, name: String, description: String, private: Boolean, autoInit: Boolean? = null) =
        BitbucketApiRequest.Post.json<BitbucketRepo>(getUrl(server, CurrentUser.urlSuffix, urlSuffix),
                BitbucketRepoRequest(name, description, private, autoInit))
          .withOperationName("create user repository")
    }

    object Orgs : Entity("/orgs") {
      @JvmOverloads
      @JvmStatic
      fun pages(server: BitbucketServerPath, pagination: BitbucketRequestPagination? = null) =
              BitbucketApiPagesLoader.Request(get(server, pagination), ::get)

      fun get(server: BitbucketServerPath, pagination: BitbucketRequestPagination? = null) =
        get(getUrl(server, CurrentUser.urlSuffix, urlSuffix, getQuery(pagination?.toString().orEmpty())))

      fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketOrg>(url).withOperationName("get user organizations")
    }

    object RepoSubs : Entity("/subscriptions") {
      @JvmStatic
      fun pages(server: BitbucketServerPath) = BitbucketApiPagesLoader.Request(get(server), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: BitbucketServerPath, pagination: BitbucketRequestPagination? = null) =
        get(getUrl(server, CurrentUser.urlSuffix, urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketRepo>(url).withOperationName("get repository subscriptions")
    }
  }

  object Organisations : Entity("/orgs") {

    object Repos : Entity("/repos") {
      @JvmStatic
      fun pages(server: BitbucketServerPath, organisation: String, pagination: BitbucketRequestPagination? = null) =
        BitbucketApiPagesLoader.Request(get(server, organisation, pagination), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: BitbucketServerPath, organisation: String, pagination: BitbucketRequestPagination? = null) =
        get(getUrl(server, Organisations.urlSuffix, "/", organisation, urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketRepo>(url).withOperationName("get organisation repositories")

      @JvmStatic
      fun create(server: BitbucketServerPath, organisation: String, name: String, description: String, private: Boolean) =
        BitbucketApiRequest.Post.json<BitbucketRepo>(getUrl(server, Organisations.urlSuffix, "/", organisation, urlSuffix),
                BitbucketRepoRequest(name, description, private, null))
          .withOperationName("create organisation repository")
    }
  }

  object Repos : Entity("/repos") {
    @JvmStatic
    fun get(server: BitbucketServerPath, username: String, repoName: String) =
      BitbucketApiRequest.Get.Optional.json<BitbucketRepoDetailed>(getUrl(server, urlSuffix, "/$username/$repoName"))
        .withOperationName("get information for repository $username/$repoName")

    @JvmStatic
    fun delete(server: BitbucketServerPath, username: String, repoName: String) =
      delete(getUrl(server, urlSuffix, "/$username/$repoName")).withOperationName("delete repository $username/$repoName")

    @JvmStatic
    fun delete(url: String) = BitbucketApiRequest.Delete.json<Unit>(url).withOperationName("delete repository at $url")

    object Branches : Entity("/branches") {
      @JvmStatic
      fun pages(server: BitbucketServerPath, username: String, repoName: String) =
        BitbucketApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: BitbucketServerPath, username: String, repoName: String, pagination: BitbucketRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketBranch>(url).withOperationName("get branches")

      @JvmStatic
      fun getProtection(repository: BBRepositoryCoordinates, branchName: String): BitbucketApiRequest<BBBranchProtectionRules> =
        BitbucketApiRequest.Get.json(getUrl(repository, urlSuffix, "/$branchName", "/protection"), "application/vnd.github.luke-cage-preview+json")
    }

    object Commits : Entity("/commits") {
      @JvmStatic
      fun getDiff(repository: BBRepositoryCoordinates, ref: String) =
        object : BitbucketApiRequest.Get<String>(getUrl(repository, urlSuffix, "/$ref"),
                             BitbucketApiContentHelper.V3_DIFF_JSON_MIME_TYPE) {
          override fun extractResult(response: BitbucketApiResponse): String {
            return response.handleBody(ThrowableConvertor {
              StreamUtil.readText(it, Charsets.UTF_8)
            })
          }
        }.withOperationName("get diff for ref")

      @JvmStatic
      fun getDiff(repository: BBRepositoryCoordinates, refA: String, refB: String) =
        object : BitbucketApiRequest.Get<String>(getUrl(repository, "/compare/$refA...$refB"),
                             BitbucketApiContentHelper.V3_DIFF_JSON_MIME_TYPE) {
          override fun extractResult(response: BitbucketApiResponse): String {
            return response.handleBody(ThrowableConvertor {
              StreamUtil.readText(it, Charsets.UTF_8)
            })
          }
        }.withOperationName("get diff between refs")
    }

    object Forks : Entity("/forks") {

      @JvmStatic
      fun create(server: BitbucketServerPath, username: String, repoName: String) =
        BitbucketApiRequest.Post.json<BitbucketRepo>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix), Any())
          .withOperationName("fork repository $username/$repoName for cuurent user")

      @JvmStatic
      fun pages(server: BitbucketServerPath, username: String, repoName: String) =
        BitbucketApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: BitbucketServerPath, username: String, repoName: String, pagination: BitbucketRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketRepo>(url).withOperationName("get forks")
    }

    object Assignees : Entity("/assignees") {

      @JvmStatic
      fun pages(server: BitbucketServerPath, username: String, repoName: String) =
        BitbucketApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: BitbucketServerPath, username: String, repoName: String, pagination: BitbucketRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketUser>(url).withOperationName("get assignees")
    }

    object Labels : Entity("/labels") {

      @JvmStatic
      fun pages(server: BitbucketServerPath, username: String, repoName: String) =
        BitbucketApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: BitbucketServerPath, username: String, repoName: String, pagination: BitbucketRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketIssueLabel>(url).withOperationName("get assignees")
    }

    object Collaborators : Entity("/collaborators") {

      @JvmStatic
      fun pages(server: BitbucketServerPath, username: String, repoName: String) =
        BitbucketApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: BitbucketServerPath, username: String, repoName: String, pagination: BitbucketRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketUserWithPermissions>(url).withOperationName("get collaborators")

      @JvmStatic
      fun add(server: BitbucketServerPath, username: String, repoName: String, collaborator: String) =
        BitbucketApiRequest.Put.json<Unit>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/", collaborator))
    }

    object Issues : Entity("/issues") {

      @JvmStatic
      fun create(server: BitbucketServerPath,
                 username: String,
                 repoName: String,
                 title: String,
                 body: String? = null,
                 milestone: Long? = null,
                 labels: List<String>? = null,
                 assignees: List<String>? = null) =
        BitbucketApiRequest.Post.json<BitbucketIssue>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix),
                               BitbucketCreateIssueRequest(title, body, milestone, labels, assignees))

      @JvmStatic
      fun pages(server: BitbucketServerPath, username: String, repoName: String,
                state: String? = null, assignee: String? = null) = BitbucketApiPagesLoader.Request(get(server, username, repoName,
                                                                                                    state, assignee), ::get)

      @JvmStatic
      fun get(server: BitbucketServerPath, username: String, repoName: String,
              state: String? = null, assignee: String? = null, pagination: BitbucketRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix,
                   BitbucketApiUrlQueryBuilder.urlQuery { param("state", state); param("assignee", assignee); param(pagination) }))

      @JvmStatic
      fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketIssue>(url).withOperationName("get issues in repository")

      @JvmStatic
      fun get(server: BitbucketServerPath, username: String, repoName: String, id: String) =
        BitbucketApiRequest.Get.Optional.json<BitbucketIssue>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/", id))

      @JvmStatic
      fun updateState(server: BitbucketServerPath, username: String, repoName: String, id: String, open: Boolean) =
        BitbucketApiRequest.Patch.json<BitbucketIssue>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/", id),
                                BitbucketChangeIssueStateRequest(if (open) "open" else "closed"))

      @JvmStatic
      fun updateAssignees(server: BitbucketServerPath, username: String, repoName: String, id: String, assignees: Collection<String>) =
        BitbucketApiRequest.Patch.json<BitbucketIssue>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/", id),
                                BitbucketAssigneesCollectionRequest(assignees))

      object Comments : Entity("/comments") {
        @JvmStatic
        fun create(repository: BBRepositoryCoordinates, issueId: Long, body: String) =
          create(repository.serverPath, repository.repositoryPath.owner, repository.repositoryPath.repository, issueId.toString(), body)

        @JvmStatic
        fun create(server: BitbucketServerPath, username: String, repoName: String, issueId: String, body: String) =
          BitbucketApiRequest.Post.json<BitbucketIssueCommentWithHtml>(
            getUrl(server, Repos.urlSuffix, "/$username/$repoName", Issues.urlSuffix, "/", issueId, urlSuffix),
            BitbucketCreateIssueCommentRequest(body),
            BitbucketApiContentHelper.V3_HTML_JSON_MIME_TYPE)

        @JvmStatic
        fun pages(server: BitbucketServerPath, username: String, repoName: String, issueId: String) =
          BitbucketApiPagesLoader.Request(get(server, username, repoName, issueId), ::get)

        @JvmStatic
        fun pages(url: String) = BitbucketApiPagesLoader.Request(get(url), ::get)

        @JvmStatic
        fun get(server: BitbucketServerPath, username: String, repoName: String, issueId: String,
                pagination: BitbucketRequestPagination? = null) =
          get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", Issues.urlSuffix, "/", issueId, urlSuffix,
                     BitbucketApiUrlQueryBuilder.urlQuery { param(pagination) }))

        @JvmStatic
        fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketIssueCommentWithHtml>(url, BitbucketApiContentHelper.V3_HTML_JSON_MIME_TYPE)
          .withOperationName("get comments for issue")
      }

      object Labels : Entity("/labels") {
        @JvmStatic
        fun replace(server: BitbucketServerPath, username: String, repoName: String, issueId: String, labels: Collection<String>) =
          BitbucketApiRequest.Put.jsonList<BitbucketIssueLabel>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", Issues.urlSuffix, "/", issueId, urlSuffix),
                                         BitbucketLabelsCollectionRequest(labels))
      }
    }

    object PullRequests : Entity("/pulls") {

      @JvmStatic
      fun getDiff(repository: BBRepositoryCoordinates, number: Long) =
        object : BitbucketApiRequest.Get<String>(getUrl(repository, urlSuffix, "/$number"),
                             BitbucketApiContentHelper.V3_DIFF_JSON_MIME_TYPE) {
          override fun extractResult(response: BitbucketApiResponse): String {
            return response.handleBody(ThrowableConvertor {
              StreamUtil.readText(it, Charsets.UTF_8)
            })
          }
        }.withOperationName("get pull request diff file")

      @JvmStatic
      fun create(server: BitbucketServerPath,
                 username: String, repoName: String,
                 title: String, description: String, head: String, base: String) =
        BitbucketApiRequest.Post.json<BitbucketPullRequestDetailed>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix),
                                             BitbucketPullRequestRequest(title, description, head, base))
          .withOperationName("create pull request in $username/$repoName")

      @JvmStatic
      fun update(serverPath: BitbucketServerPath, username: String, repoName: String, number: Long,
                 title: String? = null,
                 body: String? = null,
                 state: BitbucketIssueState? = null,
                 base: String? = null,
                 maintainerCanModify: Boolean? = null) =
        BitbucketApiRequest.Patch.json<BitbucketPullRequestDetailed>(getUrl(serverPath, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/$number"),
                                              BitbucketPullUpdateRequest(title, body, state, base, maintainerCanModify))
          .withOperationName("update pull request $number")

      @JvmStatic
      fun update(url: String,
                 title: String? = null,
                 body: String? = null,
                 state: BitbucketIssueState? = null,
                 base: String? = null,
                 maintainerCanModify: Boolean? = null) =
        BitbucketApiRequest.Patch.json<BitbucketPullRequestDetailed>(url, BitbucketPullUpdateRequest(title, body, state, base, maintainerCanModify))
          .withOperationName("update pull request")

      @JvmStatic
      fun merge(server: BitbucketServerPath, repoPath: BBRepositoryPath, number: Long,
                commitSubject: String, commitBody: String, headSha: String) =
        BitbucketApiRequest.Put.json<Unit>(getUrl(server, Repos.urlSuffix, "/$repoPath", urlSuffix, "/$number", "/merge"),
                       BitbucketPullRequestMergeRequest(commitSubject, commitBody, headSha, BitbucketPullRequestMergeMethod.merge))
          .withOperationName("merge pull request ${number}")

      @JvmStatic
      fun squashMerge(server: BitbucketServerPath, repoPath: BBRepositoryPath, number: Long,
                      commitSubject: String, commitBody: String, headSha: String) =
        BitbucketApiRequest.Put.json<Unit>(getUrl(server, Repos.urlSuffix, "/$repoPath", urlSuffix, "/$number", "/merge"),
                       BitbucketPullRequestMergeRequest(commitSubject, commitBody, headSha, BitbucketPullRequestMergeMethod.squash))
          .withOperationName("squash and merge pull request ${number}")

      @JvmStatic
      fun rebaseMerge(server: BitbucketServerPath, repoPath: BBRepositoryPath, number: Long,
                      headSha: String) =
        BitbucketApiRequest.Put.json<Unit>(getUrl(server, Repos.urlSuffix, "/$repoPath", urlSuffix, "/$number", "/merge"),
                       BitbucketPullRequestMergeRebaseRequest(headSha))
          .withOperationName("rebase and merge pull request ${number}")

      @JvmStatic
      fun getListETag(server: BitbucketServerPath, repoPath: BBRepositoryPath) =
        object : BitbucketApiRequest.Get<String?>(getUrl(server, Repos.urlSuffix, "/$repoPath", urlSuffix,
                                     BitbucketApiUrlQueryBuilder.urlQuery { param(BitbucketRequestPagination(pageSize = 1)) })) {
          override fun extractResult(response: BitbucketApiResponse) = response.findHeader("ETag")
        }.withOperationName("get pull request list ETag")

      object Reviewers : Entity("/requested_reviewers") {
        @JvmStatic
        fun add(server: BitbucketServerPath, username: String, repoName: String, number: Long,
                reviewers: Collection<String>, teamReviewers: List<String>) =
          BitbucketApiRequest.Post.json<Unit>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", PullRequests.urlSuffix, "/$number", urlSuffix),
                          BitbucketReviewersCollectionRequest(reviewers, teamReviewers))

        @JvmStatic
        fun remove(server: BitbucketServerPath, username: String, repoName: String, number: Long,
                   reviewers: Collection<String>, teamReviewers: List<String>) =
          BitbucketApiRequest.Delete.json<Unit>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", PullRequests.urlSuffix, "/$number", urlSuffix),
                            BitbucketReviewersCollectionRequest(reviewers, teamReviewers))
      }

      object Commits : Entity("/commits") {
        @JvmStatic
        fun pages(repository: BBRepositoryCoordinates, number: Long) =
          BitbucketApiPagesLoader.Request(get(repository, number), ::get)

        @JvmStatic
        fun pages(url: String) = BitbucketApiPagesLoader.Request(get(url), ::get)

        @JvmStatic
        fun get(repository: BBRepositoryCoordinates, number: Long,
                pagination: BitbucketRequestPagination? = null) =
          get(getUrl(repository, PullRequests.urlSuffix, "/$number", urlSuffix,
                     BitbucketApiUrlQueryBuilder.urlQuery { param(pagination) }))

        @JvmStatic
        fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketCommit>(url)
          .withOperationName("get commits for pull request")
      }

      object Comments : Entity("/comments") {
        @JvmStatic
        fun pages(server: BitbucketServerPath, username: String, repoName: String, number: Long) =
          BitbucketApiPagesLoader.Request(get(server, username, repoName, number), ::get)

        @JvmStatic
        fun pages(url: String) = BitbucketApiPagesLoader.Request(get(url), ::get)

        @JvmStatic
        fun get(server: BitbucketServerPath, username: String, repoName: String, number: Long,
                pagination: BitbucketRequestPagination? = null) =
          get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", PullRequests.urlSuffix, "/$number", urlSuffix,
                     BitbucketApiUrlQueryBuilder.urlQuery { param(pagination) }))

        @JvmStatic
        fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketPullRequestCommentWithHtml>(url, BitbucketApiContentHelper.V3_HTML_JSON_MIME_TYPE)
          .withOperationName("get comments for pull request")

        @JvmStatic
        fun createReply(repository: BBRepositoryCoordinates, pullRequest: Long, commentId: Long, body: String) =
          BitbucketApiRequest.Post.json<BitbucketPullRequestCommentWithHtml>(
            getUrl(repository, PullRequests.urlSuffix, "/$pullRequest", "/comments/$commentId/replies"),
            mapOf("body" to body),
            BitbucketApiContentHelper.V3_HTML_JSON_MIME_TYPE).withOperationName("reply to pull request review comment")

        @JvmStatic
        fun create(repository: BBRepositoryCoordinates, pullRequest: Long,
                   commitSha: String, filePath: String, diffLine: Int,
                   body: String) =
          BitbucketApiRequest.Post.json<BitbucketPullRequestCommentWithHtml>(
            getUrl(repository, PullRequests.urlSuffix, "/$pullRequest", "/comments"),
            mapOf("body" to body,
                  "commit_id" to commitSha,
                  "path" to filePath,
                  "position" to diffLine),
            BitbucketApiContentHelper.V3_HTML_JSON_MIME_TYPE).withOperationName("create pull request review comment")
      }
    }
  }

  object Gists : Entity("/gists") {
    @JvmStatic
    fun create(server: BitbucketServerPath,
               contents: List<BitbucketGistRequest.FileContent>, description: String, public: Boolean) =
      BitbucketApiRequest.Post.json<BitbucketGist>(getUrl(server, urlSuffix),
                            BitbucketGistRequest(contents, description, public))
        .withOperationName("create gist")

    @JvmStatic
    fun get(server: BitbucketServerPath, id: String) = BitbucketApiRequest.Get.Optional.json<BitbucketGist>(getUrl(server, urlSuffix, "/$id"))
      .withOperationName("get gist $id")

    @JvmStatic
    fun delete(server: BitbucketServerPath, id: String) = BitbucketApiRequest.Delete.json<Unit>(getUrl(server, urlSuffix, "/$id"))
      .withOperationName("delete gist $id")
  }

  object Search : Entity("/search") {
    object Issues : Entity("/issues") {
      @JvmStatic
      fun pages(server: BitbucketServerPath, repoPath: BBRepositoryPath?, state: String?, assignee: String?, query: String?) =
        BitbucketApiPagesLoader.Request(get(server, repoPath, state, assignee, query), ::get)

      @JvmStatic
      fun get(server: BitbucketServerPath, repoPath: BBRepositoryPath?, state: String?, assignee: String?, query: String?,
              pagination: BitbucketRequestPagination? = null) =
        get(getUrl(server, Search.urlSuffix, urlSuffix,
                   BitbucketApiUrlQueryBuilder.urlQuery {
                     param("q", BitbucketApiSearchQueryBuilder.searchQuery {
                       qualifier("repo", repoPath?.toString().orEmpty())
                       qualifier("state", state)
                       qualifier("assignee", assignee)
                       query(query)
                     })
                     param(pagination)
                   }))

      @JvmStatic
      fun get(server: BitbucketServerPath, query: String, pagination: BitbucketRequestPagination? = null) =
        get(getUrl(server, Search.urlSuffix, urlSuffix,
                   BitbucketApiUrlQueryBuilder.urlQuery {
                     param("q", query)
                     param(pagination)
                   }))


      @JvmStatic
      fun get(url: String) = BitbucketApiRequest.Get.jsonSearchPage<BitbucketSearchedIssue>(url).withOperationName("search issues in repository")
    }
  }

  object Auth : Entity("/authorizations") {
    @JvmStatic
    fun create(server: BitbucketServerPath, scopes: List<String>, note: String) =
      BitbucketApiRequest.Post.json<BitbucketAuthorization>(getUrl(server, urlSuffix),
                                     BitbucketAuthorizationCreateRequest(scopes, note, null))
        .withOperationName("create authorization $note")

    @JvmStatic
    fun get(server: BitbucketServerPath, pagination: BitbucketRequestPagination? = null) =
      get(getUrl(server, urlSuffix,
                 BitbucketApiUrlQueryBuilder.urlQuery { param(pagination) }))

    @JvmStatic
    fun get(url: String) = BitbucketApiRequest.Get.jsonPage<BitbucketAuthorization>(url)
      .withOperationName("get authorizations")

    @JvmStatic
    fun pages(server: BitbucketServerPath, pagination: BitbucketRequestPagination? = null) =
      BitbucketApiPagesLoader.Request(get(server, pagination), ::get)
  }

  abstract class Entity(val urlSuffix: String)

  private fun getUrl(server: BitbucketServerPath, suffix: String) = server.toApiUrl() + suffix

  private fun getUrl(repository: BBRepositoryCoordinates, vararg suffixes: String) =
    getUrl(repository.serverPath, Repos.urlSuffix, "/", repository.repositoryPath.toString(), *suffixes)

  fun getUrl(server: BitbucketServerPath, vararg suffixes: String) = StringBuilder(server.toApiUrl()).append(*suffixes).toString()

  private fun getQuery(vararg queryParts: String): String {
    val builder = StringBuilder()
    for (part in queryParts) {
      if (part.isEmpty()) continue
      if (builder.isEmpty()) builder.append("?")
      else builder.append("&")
      builder.append(part)
    }
    return builder.toString()
  }
}