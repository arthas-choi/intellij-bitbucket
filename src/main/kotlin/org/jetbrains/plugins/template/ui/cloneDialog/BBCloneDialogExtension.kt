// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.ui.cloneDialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutor
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutorManager
import org.jetbrains.plugins.template.authentication.BitbucketAuthenticationManager
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccountInformationProvider
import org.jetbrains.plugins.template.util.BitbucketUtil
import org.jetbrains.plugins.template.util.CachingBitbucketUserAvatarLoader
import org.jetbrains.plugins.template.util.BitbucketImageResizer
import javax.swing.Icon

class BBCloneDialogExtension : VcsCloneDialogExtension {
  private val authenticationManager = BitbucketAuthenticationManager.getInstance()

  override fun getName() = BitbucketUtil.SERVICE_DISPLAY_NAME

  override fun getIcon(): Icon = AllIcons.Vcs.Vendors.Github

  override fun getAdditionalStatusLines(): List<VcsCloneDialogExtensionStatusLine> {
    if (!authenticationManager.hasAccounts()) {
      return listOf(VcsCloneDialogExtensionStatusLine.greyText("No accounts"))
    }

    val list = ArrayList<VcsCloneDialogExtensionStatusLine>()
    for (account in authenticationManager.getAccounts()) {
      val accName = if (account.server.isGithubDotCom) account.name else ("${account.server.host}/${account.name}")
      list.add(VcsCloneDialogExtensionStatusLine.greyText(accName))
    }
    return list
  }

  override fun createMainComponent(project: Project): VcsCloneDialogExtensionComponent {
    throw AssertionError("Shouldn't be called")
  }

  override fun createMainComponent(project: Project, modalityState: ModalityState): VcsCloneDialogExtensionComponent {
    return BBCloneDialogExtensionComponent(project,
            BitbucketAuthenticationManager.getInstance(),
            BitbucketApiRequestExecutorManager.getInstance(),
            BitbucketApiRequestExecutor.Factory.getInstance(),
                                           BitbucketAccountInformationProvider.getInstance(),
                                           CachingBitbucketUserAvatarLoader.getInstance(),
                                           BitbucketImageResizer.getInstance()
    )
  }
}