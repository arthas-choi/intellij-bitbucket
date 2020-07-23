// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.comment.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorEmbeddedComponentManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.view.FontLayoutService
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.CalledInAwt
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class EditorComponentInlaysManager(val editor: EditorImpl) : Disposable {

  private val managedInlays = mutableMapOf<ComponentWrapper, Disposable>()
  private val editorWidthWatcher = EditorTextWidthWatcher()

  init {
    editor.scrollPane.viewport.addComponentListener(editorWidthWatcher)
    Disposer.register(this, Disposable {
      editor.scrollPane.viewport.removeComponentListener(editorWidthWatcher)
    })

    EditorUtil.disposeWithEditor(editor, this)
  }

  @CalledInAwt
  fun insertAfter(lineIndex: Int, component: JComponent): Disposable? {
    if (Disposer.isDisposed(this)) return null

    val wrappedComponent = ComponentWrapper(component)
    val offset = editor.document.getLineEndOffset(lineIndex)

    return EditorEmbeddedComponentManager.getInstance()
      .addComponent(editor, wrappedComponent,
                    EditorEmbeddedComponentManager.Properties(EditorEmbeddedComponentManager.ResizePolicy.none(),
                                                              null,
                                                              true,
                                                              false,
                                                              0,
                                                              offset))?.also {
        managedInlays[wrappedComponent] = it
        Disposer.register(it, Disposable { managedInlays.remove(wrappedComponent) })
      }
  }

  private inner class ComponentWrapper(private val component: JComponent) : JBScrollPane(component) {
    init {
      isOpaque = false
      viewport.isOpaque = false

      border = JBUI.Borders.empty()
      viewportBorder = JBUI.Borders.empty()

      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      verticalScrollBar.preferredSize = Dimension(0, 0)
      setViewportView(component)

      component.addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) = dispatchEvent(ComponentEvent(component, ComponentEvent.COMPONENT_RESIZED))
      })
    }

    override fun getPreferredSize(): Dimension {
      return Dimension(editorWidthWatcher.editorTextWidth, component.preferredSize.height)
    }
  }

  override fun dispose() {
    managedInlays.values.forEach(Disposer::dispose)
  }

  private inner class EditorTextWidthWatcher : ComponentAdapter() {

    var editorTextWidth: Int = 0

    private val maximumEditorTextWidth: Int
    private val verticalScrollbarFlipped: Boolean

    init {
      val metrics = editor.getFontMetrics(Font.PLAIN)
      val spaceWidth = FontLayoutService.getInstance().charWidth2D(metrics, ' '.toInt())
      // -4 to create some space
      maximumEditorTextWidth = ceil(spaceWidth * (editor.settings.getRightMargin(editor.project)) - 4).toInt()

      val scrollbarFlip = editor.scrollPane.getClientProperty(JBScrollPane.Flip::class.java)
      verticalScrollbarFlipped = scrollbarFlip == JBScrollPane.Flip.HORIZONTAL || scrollbarFlip == JBScrollPane.Flip.BOTH
    }

    override fun componentResized(e: ComponentEvent) = updateWidthForAllInlays()
    override fun componentHidden(e: ComponentEvent) = updateWidthForAllInlays()
    override fun componentShown(e: ComponentEvent) = updateWidthForAllInlays()

    private fun updateWidthForAllInlays() {
      val newWidth = calcWidth()
      if (editorTextWidth == newWidth) return
      editorTextWidth = newWidth

      managedInlays.keys.forEach {
        it.dispatchEvent(ComponentEvent(it, ComponentEvent.COMPONENT_RESIZED))
        it.invalidate()
      }
    }

    private fun calcWidth(): Int {
      val visibleEditorTextWidth = editor.scrollPane.viewport.width - getVerticalScrollbarWidth() - getGutterTextGap()
      return min(max(visibleEditorTextWidth, 0), maximumEditorTextWidth)
    }

    private fun getVerticalScrollbarWidth(): Int {
      val width = editor.scrollPane.verticalScrollBar.width
      return if (!verticalScrollbarFlipped) width * 2 else width
    }

    private fun getGutterTextGap(): Int {
      return if (verticalScrollbarFlipped) {
        val gutter = (editor as EditorEx).gutterComponentEx
        gutter.width - gutter.whitespaceSeparatorOffset
      }
      else 0
    }
  }
}