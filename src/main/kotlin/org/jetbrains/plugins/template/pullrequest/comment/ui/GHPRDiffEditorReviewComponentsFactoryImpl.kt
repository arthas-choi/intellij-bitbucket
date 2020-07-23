// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.comment.ui

import com.intellij.diff.util.Side
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI





import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent
import javax.swing.JPanel

class GHPRDiffEditorReviewComponentsFactoryImpl
internal constructor(private val reviewService: GHPRReviewServiceAdapter,
                     private val createCommentParametersHelper: GHPRCreateDiffCommentParametersHelper,
                     private val avatarIconsProviderFactory: CachingGithubAvatarIconsProvider.Factory,
                     private val currentUser: GHUser)
  : GHPRDiffEditorReviewComponentsFactory {

  override fun createThreadComponent(thread: GHPRReviewThreadModel): JComponent {
    val wrapper = RoundedPanel(BorderLayout()).apply {
      border = JBUI.Borders.empty(2, 0)
    }
    val avatarIconsProvider = avatarIconsProviderFactory.create(GithubUIUtil.avatarSize, wrapper)
    val component = GHPRReviewThreadComponent.create(thread, reviewService, avatarIconsProvider, currentUser).apply {
      border = JBUI.Borders.empty(8, 8)
    }
    wrapper.add(component, BorderLayout.NORTH)
    component.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) =
        wrapper.dispatchEvent(ComponentEvent(component, ComponentEvent.COMPONENT_RESIZED))
    })
    return wrapper
  }

  override fun createCommentComponent(side: Side, line: Int, hideCallback: () -> Unit): JComponent {
    val wrapper = RoundedPanel(BorderLayout()).apply {
      border = JBUI.Borders.empty(2, 0)
    }
    val avatarIconsProvider = avatarIconsProviderFactory.create(GithubUIUtil.avatarSize, wrapper)

    val model = GHPRSubmittableTextField.Model {
      val commitSha = createCommentParametersHelper.commitSha
      val filePath = createCommentParametersHelper.filePath
      val diffLine = createCommentParametersHelper.findPosition(side, line) ?: error("Can't determine comment position")
      reviewService.addComment(EmptyProgressIndicator(), it, commitSha, filePath, diffLine).successOnEdt {
        hideCallback()
      }
    }

    val commentField = GHPRSubmittableTextField.create(model, avatarIconsProvider, currentUser, "Comment") {
      hideCallback()
    }.apply {
      border = JBUI.Borders.empty(8)
    }

    wrapper.add(commentField, BorderLayout.NORTH)
    commentField.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) =
        wrapper.dispatchEvent(ComponentEvent(commentField, ComponentEvent.COMPONENT_RESIZED))
    })
    return wrapper
  }

  private class RoundedPanel(layout: LayoutManager?) : JPanel(layout) {
    private var borderLineColor: Color? = null

    init {
      cursor = Cursor.getDefaultCursor()
      updateColors()
    }

    override fun updateUI() {
      super.updateUI()
      updateColors()
    }

    private fun updateColors() {
      val scheme = EditorColorsManager.getInstance().globalScheme
      background = scheme.defaultBackground
      borderLineColor = scheme.getColor(EditorColors.TEARLINE_COLOR)
    }

    override fun paintComponent(g: Graphics) {
      GraphicsUtil.setupRoundedBorderAntialiasing(g)

      val g2 = g as Graphics2D
      val rect = Rectangle(size)
      JBInsets.removeFrom(rect, insets)
      // 2.25 scale is a @#$!% so we adjust sizes manually
      val rectangle2d = RoundRectangle2D.Float(rect.x.toFloat() + 0.5f, rect.y.toFloat() + 0.5f,
                                               rect.width.toFloat() - 1f, rect.height.toFloat() - 1f,
                                               10f, 10f)
      g2.color = background
      g2.fill(rectangle2d)
      borderLineColor?.let {
        g2.color = borderLineColor
        g2.draw(rectangle2d)
      }
    }
  }
}