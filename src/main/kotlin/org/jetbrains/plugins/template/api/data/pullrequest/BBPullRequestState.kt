// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest

import org.jetbrains.plugins.template.api.data.BitbucketIssueState

enum class BBPullRequestState {
  CLOSED, MERGED, OPEN;

  fun asIssueState(): BitbucketIssueState = if(this == CLOSED || this == MERGED) BitbucketIssueState.closed else BitbucketIssueState.open
}