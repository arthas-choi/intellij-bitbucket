// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.UIUtil
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutorManager
import org.jetbrains.plugins.template.authentication.BitbucketAuthenticationManager
import org.jetbrains.plugins.template.authentication.accounts.AccountTokenChangedListener
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccountManager
import org.jetbrains.plugins.template.authentication.ui.BitbucketChooseAccountDialog
import org.jetbrains.plugins.template.ui.util.DisposingWrapper
import org.jetbrains.plugins.template.util.BitbucketUIUtil
import org.jetbrains.plugins.template.util.GitRemoteUrlCoordinates


internal class BBPRAccountsComponent(private val authManager: BitbucketAuthenticationManager,
                                     private val project: Project,
                                     private val remoteUrl: GitRemoteUrlCoordinates,
                                     parentDisposable: Disposable)
  : DisposingWrapper(parentDisposable) {

  private val requestExecutorManager by lazy(LazyThreadSafetyMode.NONE) { BitbucketApiRequestExecutorManager.getInstance() }
  private var selectedAccount: BitbucketAccount? = null

  init {
    background = UIUtil.getListBackground()

    ApplicationManager.getApplication().messageBus.connect(parentDisposable)
      .subscribe(BitbucketAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC,
                 object : AccountTokenChangedListener {
                   override fun tokenChanged(account: BitbucketAccount) {
                     update()
                   }
                 })
    //TODO: listen to default account changes?
    update()
  }

  private fun update() {
    if (selectedAccount != null) return

    val accounts = authManager.getAccounts().filter { it.server.matches(remoteUrl.url) }

    if (accounts.size == 1) {
      setActualContent(accounts.single())
      return
    }

    val defaultAccount = accounts.find { it == authManager.getDefaultAccount(project) }
    if (defaultAccount != null) {
      setActualContent(defaultAccount)
      return
    }

    if (accounts.isNotEmpty()) {
      showChooseAccountPanel(accounts)
    }
    else {
      showLoginPanel()
    }
  }

  private fun showLoginPanel() {
    setCenteredContent(BitbucketUIUtil.createNoteWithAction(::requestNewAccount).apply {
      append("Log in", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES, Runnable { requestNewAccount() })
      append(" to GitHub to view pull requests", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    })
  }

  private fun requestNewAccount() {
    authManager.requestNewAccount(project)
    IdeFocusManager.getInstance(project).requestFocusInProject(this@BBPRAccountsComponent, project)
  }

  private fun showChooseAccountPanel(accounts: List<BitbucketAccount>) {
    setCenteredContent(BitbucketUIUtil.createNoteWithAction { chooseAccount(accounts) }.apply {
      append("Select", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES, Runnable { chooseAccount(accounts) })
      append(" GitHub account to view pull requests", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    })
  }

  private fun chooseAccount(accounts: List<BitbucketAccount>) {
    val dialog = BitbucketChooseAccountDialog(project, null, accounts, null, true, true)
    if (dialog.showAndGet()) {
      setActualContent(dialog.account)
      IdeFocusManager.getInstance(project).requestFocusInProject(this@BBPRAccountsComponent, project)
    }
  }

  private fun setActualContent(account: BitbucketAccount) {
    selectedAccount = account
    val disposable = Disposer.newDisposable()
    setContent(GHPRRequestExecutorComponent(requestExecutorManager, project, remoteUrl, account, disposable), disposable)
  }
}