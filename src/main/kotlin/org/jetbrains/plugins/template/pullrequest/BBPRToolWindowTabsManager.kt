// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest

import com.intellij.dvcs.repo.VcsRepositoryMappingListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import org.jetbrains.annotations.CalledInAwt
import org.jetbrains.plugins.template.authentication.accounts.AccountRemovedListener
import org.jetbrains.plugins.template.authentication.accounts.AccountTokenChangedListener
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount
import org.jetbrains.plugins.template.pullrequest.config.BitbucketPullRequestsProjectUISettings
import org.jetbrains.plugins.template.util.BitbucketGitHelper
import org.jetbrains.plugins.template.util.GitRemoteUrlCoordinates
import org.jetbrains.plugins.template.util.CollectionDelta


import kotlin.properties.Delegates.observable

@Service
internal class BBPRToolWindowTabsManager(private val project: Project) {
  private val gitHelper = BitbucketGitHelper.getInstance()
  private val settings = BitbucketPullRequestsProjectUISettings.getInstance(project)

  private val contentManager by lazy(LazyThreadSafetyMode.NONE) {
    GHPRToolWindowsTabsContentManager(project, ChangesViewContentManager.getInstance(project))
  }

  private var remoteUrls by observable(setOf<GitRemoteUrlCoordinates>()) { _, oldValue, newValue ->
    val delta = CollectionDelta(oldValue, newValue)
    for (item in delta.removedItems) {
      contentManager.removeTab(item)
    }
    for (item in delta.newItems) {
      contentManager.addTab(item, Disposable {
        //means that tab closed by user
        if (gitHelper.getPossibleRemoteUrlCoordinates(project).contains(item)) settings.addHiddenUrl(item.url)
        ApplicationManager.getApplication().invokeLater(::updateRemoteUrls) { project.isDisposed }
      })
    }
  }

  @CalledInAwt
  fun showTab(remoteUrl: GitRemoteUrlCoordinates) {
    settings.removeHiddenUrl(remoteUrl.url)
    updateRemoteUrls()

    contentManager.focusTab(remoteUrl)
  }

  private fun updateRemoteUrls() {
    remoteUrls = gitHelper.getPossibleRemoteUrlCoordinates(project).filter {
      !settings.getHiddenUrls().contains(it.url)
    }.toSet()
  }

  class RemoteUrlsListener(private val project: Project)
    : VcsRepositoryMappingListener, GitRepositoryChangeListener {

    override fun mappingChanged() = runInEdt(project) { updateRemotes(project) }
    override fun repositoryChanged(repository: GitRepository) = runInEdt(project) { updateRemotes(project) }
  }

  class AccountsListener : AccountRemovedListener, AccountTokenChangedListener {
    override fun accountRemoved(removedAccount: BitbucketAccount) = updateRemotes()
    override fun tokenChanged(account: BitbucketAccount) = updateRemotes()

    private fun updateRemotes() = runInEdt {
      for (project in ProjectManager.getInstance().openProjects) {
        updateRemotes(project)
      }
    }
  }

  companion object {
    private inline fun runInEdt(project: Project, crossinline runnable: () -> Unit) {
      val application = ApplicationManager.getApplication()
      if (application.isDispatchThread) runnable()
      else application.invokeLater({ runnable() }) { project.isDisposed }
    }

    private fun updateRemotes(project: Project) = project.service<BBPRToolWindowTabsManager>().updateRemoteUrls()
  }
}
