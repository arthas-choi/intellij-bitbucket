// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.util

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import org.jetbrains.plugins.template.api.BBRepositoryCoordinates
import org.jetbrains.plugins.template.api.BBRepositoryPath
import org.jetbrains.plugins.template.api.BitbucketServerPath
import org.jetbrains.plugins.template.authentication.BitbucketAuthenticationManager


/**
 * Utilities for Github-Git interactions
 *
 * accessible url - url that matches at least one registered account
 * possible url - accessible urls + urls that match github.com + urls that match server saved in old settings
 */
@Service
class BitbucketGitHelper {
  fun getRemoteUrl(server: BitbucketServerPath, repoPath: BBRepositoryPath): String {
    return getRemoteUrl(server, repoPath.owner, repoPath.repository)
  }

  fun getRemoteUrl(server: BitbucketServerPath, user: String, repo: String): String {
    return if (BitbucketSettings.getInstance().isCloneGitUsingSsh) {
      "git@${server.host}:${server.suffix?.substring(1).orEmpty()}/$user/$repo.git"
    }
    else {
      "https://${server.host}${server.suffix.orEmpty()}/$user/$repo.git"
    }
  }

  fun getAccessibleRemoteUrls(repository: GitRepository): List<String> {
    return repository.getRemoteUrls().filter(::isRemoteUrlAccessible)
  }

  fun hasAccessibleRemotes(repository: GitRepository): Boolean {
    return repository.getRemoteUrls().any(::isRemoteUrlAccessible)
  }

  private fun isRemoteUrlAccessible(url: String) = BitbucketAuthenticationManager.getInstance().getAccounts().find { it.server.matches(url) } != null

  fun getPossibleRepositories(repository: GitRepository): Set<BBRepositoryCoordinates> {
    val knownServers = getKnownGithubServers()
    return repository.getRemoteUrls().mapNotNull { url ->
      knownServers.find { it.matches(url) }
        ?.let { server -> BitbucketUrlUtil.getUserAndRepositoryFromRemoteUrl(url)?.let { BBRepositoryCoordinates(server, it) } }
    }.toSet()
  }

  fun getPossibleRemoteUrlCoordinates(project: Project): Set<GitRemoteUrlCoordinates> {
    val repositories = project.service<GitRepositoryManager>().repositories
    if (repositories.isEmpty()) return emptySet()

    val knownServers = getKnownGithubServers()

    return repositories.flatMap { repo ->
      repo.remotes.flatMap { remote ->
        remote.urls.mapNotNull { url ->
          if (knownServers.any { it.matches(url) }) GitRemoteUrlCoordinates(url, remote, repo) else null
        }
      }
    }.toSet()
  }

  fun havePossibleRemotes(project: Project): Boolean {
    val repositories = project.service<GitRepositoryManager>().repositories
    if (repositories.isEmpty()) return false

    val knownServers = getKnownGithubServers()
    return repositories.any { repo -> repo.getRemoteUrls().any { url -> knownServers.any { it.matches(url) } } }
  }

  private fun getKnownGithubServers(): Set<BitbucketServerPath> {
    val registeredServers = mutableSetOf(BitbucketServerPath.DEFAULT_SERVER)
    BitbucketAccountsMigrationHelper.getInstance().getOldServer()?.run(registeredServers::add)
    BitbucketAuthenticationManager.getInstance().getAccounts().mapTo(registeredServers) { it.server }
    return registeredServers
  }

  private fun GitRepository.getRemoteUrls() = remotes.map { it.urls }.flatten()

  companion object {
    @JvmStatic
    fun findGitRepository(project: Project, file: VirtualFile? = null): GitRepository? {
      val manager = GitUtil.getRepositoryManager(project)
      val repositories = manager.repositories
      if (repositories.size == 0) {
        return null
      }
      if (repositories.size == 1) {
        return repositories[0]
      }
      if (file != null) {
        val repository = manager.getRepositoryForFileQuick(file)
        if (repository != null) {
          return repository
        }
      }
      return manager.getRepositoryForFileQuick(project.baseDir)
    }

    @JvmStatic
    fun getInstance(): BitbucketGitHelper {
      return service()
    }
  }
}