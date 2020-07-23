// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.ui.cloneDialog

import com.intellij.ui.CellRendererPanel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JList

class BBRepositoryListCellRenderer(private val authenticationManager: GithubAuthenticationManager) : ColoredListCellRenderer<BBRepositoryListItem>() {
  private val nameRenderer = AccountNameRenderer()

  override fun getListCellRendererComponent(list: JList<out BBRepositoryListItem>,
                                            value: BBRepositoryListItem,
                                            index: Int,
                                            selected: Boolean,
                                            hasFocus: Boolean): Component {
    val component = super.getListCellRendererComponent(list, value, index, selected, hasFocus)
    if (showAccountNameAbove(list, index)) {
      val name = with(value) {
        if (account.server.isGithubDotCom) account.name else ("${account.server.host}/${account.name}")
      }
      return nameRenderer.withName(name, component, index != 0)
    }
    return component
  }

  private fun showAccountNameAbove(list: JList<out BBRepositoryListItem>, index: Int): Boolean {
    return authenticationManager.getAccounts().size > 1
           && (index == 0 || list.model.getElementAt(index).account != list.model.getElementAt(index - 1).account)
  }

  override fun customizeCellRenderer(list: JList<out BBRepositoryListItem>,
                                     value: BBRepositoryListItem,
                                     index: Int,
                                     selected: Boolean,
                                     hasFocus: Boolean) = value.customizeRenderer(this, list)


  private class AccountNameRenderer : CellRendererPanel() {
    private val titleLabel = SimpleColoredComponent().apply {
      background = UIUtil.getListBackground()
    }
    private val topLine = JBUI.Borders.customLine(JBUI.CurrentTheme.BigPopup.listSeparatorColor(), 1, 0, 0, 0)
    private val borderLayout = BorderLayout()

    init {
      layout = borderLayout
      add(titleLabel, BorderLayout.NORTH)
      background = UIUtil.getListBackground()
    }

    fun withName(title: String, itemContent: Component, withBorder: Boolean): AccountNameRenderer {
      titleLabel.border = null
      titleLabel.clear()
      titleLabel.append(title, SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)
      if (withBorder) {
        titleLabel.border = topLine
      }
      val prevContent = borderLayout.getLayoutComponent(BorderLayout.CENTER)
      if (prevContent != null) {
        remove(prevContent)
      }
      add(itemContent, BorderLayout.CENTER)
      return this
    }
  }
}

