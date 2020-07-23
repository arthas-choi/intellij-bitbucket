// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest.timeline

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.plugins.template.api.data.BBActor
import java.util.*

class BBPRReviewDismissedEvent(override val actor: BBActor?,
                               override val createdAt: Date,
                               val dismissalMessageHTML: String?,
                               @JsonProperty("review") review: ReviewAuthor?)
  : BBPRTimelineEvent.Complex {

  val reviewAuthor = review?.author

  class ReviewAuthor(val author: BBActor?)
}
