// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.comment.ui

import com.intellij.util.EventDispatcher

import java.util.*

class GHPRReviewCommentModel(val id: String, dateCreated: Date, body: String,
                             authorUsername: String?, authorLinkUrl: String?, authorAvatarUrl: String?,
                             val canBeDeleted: Boolean, val canBeUpdated: Boolean) {

  private val changeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  var dateCreated = dateCreated
    private set
  var body = body
    private set
  var authorUsername = authorUsername
    private set
  var authorLinkUrl = authorLinkUrl
    private set
  var authorAvatarUrl = authorAvatarUrl
    private set

  fun update(comment: GHPullRequestReviewComment): Boolean {
    if (comment.id != id) throw IllegalArgumentException("Can't update comment data from different comment")

    var updated = false
    dateCreated = comment.createdAt

    if (body != comment.bodyHtml)
      updated = true
    body = comment.bodyHtml

    if (authorUsername != comment.author?.login)
      updated = true
    authorUsername = comment.author?.login
    authorLinkUrl = comment.author?.url
    authorAvatarUrl = comment.author?.avatarUrl

    if (updated) changeEventDispatcher.multicaster.eventOccurred()
    return updated
  }

  fun addChangesListener(listener: () -> Unit) = SimpleEventListener.addListener(changeEventDispatcher, listener)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GHPRReviewCommentModel) return false

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }


  companion object {
    fun convert(comment: GHPullRequestReviewComment): GHPRReviewCommentModel =
      GHPRReviewCommentModel(comment.id, comment.createdAt, comment.bodyHtml,
                             comment.author?.login, comment.author?.url,
                             comment.author?.avatarUrl,
                             comment.viewerCanDelete, comment.viewerCanUpdate)
  }
}