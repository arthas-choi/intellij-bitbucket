// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest.timeline

import org.jetbrains.plugins.template.api.data.BBActor
import java.util.*

class BBPRHeadRefRestoredEvent(override val actor: BBActor?,
                               override val createdAt: Date)
  : BBPRTimelineEvent.Branch
