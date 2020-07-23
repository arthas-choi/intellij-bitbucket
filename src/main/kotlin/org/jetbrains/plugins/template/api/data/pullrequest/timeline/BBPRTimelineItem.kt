// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest.timeline

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.jetbrains.plugins.template.api.data.BBIssueComment
import org.jetbrains.plugins.template.api.data.pullrequest.BBPullRequestCommit
import org.jetbrains.plugins.template.api.data.pullrequest.BBPullRequestReview


/*REQUIRED
IssueComment
PullRequestCommit (Commit in GHE)
PullRequestReview

RenamedTitleEvent
ClosedEvent | ReopenedEvent | MergedEvent
AssignedEvent | UnassignedEvent
LabeledEvent | UnlabeledEvent
ReviewRequestedEvent | ReviewRequestRemovedEvent
ReviewDismissedEvent

BaseRefChangedEvent | BaseRefForcePushedEvent
HeadRefDeletedEvent | HeadRefForcePushedEvent | HeadRefRestoredEvent
*/
/*MAYBE
LockedEvent | UnlockedEvent

CommentDeletedEvent
???PullRequestCommitCommentThread
???PullRequestReviewThread
AddedToProjectEvent
ConvertedNoteToIssueEvent
RemovedFromProjectEvent
MovedColumnsInProjectEvent

TransferredEvent
UserBlockedEvent

PullRequestRevisionMarker

DeployedEvent
DeploymentEnvironmentChangedEvent
PullRequestReviewThread
PinnedEvent | UnpinnedEvent
SubscribedEvent | UnsubscribedEvent
MilestonedEvent | DemilestonedEvent
MentionedEvent | ReferencedEvent | CrossReferencedEvent
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename", visible = false,
              defaultImpl = BBPRTimelineItem.Unknown::class)
@JsonSubTypes(
  JsonSubTypes.Type(name = "IssueComment", value = BBIssueComment::class),
  JsonSubTypes.Type(name = "PullRequestCommit", value = BBPullRequestCommit::class),
  JsonSubTypes.Type(name = "PullRequestReview", value = BBPullRequestReview::class),

  JsonSubTypes.Type(name = "ReviewDismissedEvent", value = BBPRReviewDismissedEvent::class),

  JsonSubTypes.Type(name = "RenamedTitleEvent", value = BBPRRenamedTitleEvent::class),

  JsonSubTypes.Type(name = "ClosedEvent", value = BBPRClosedEvent::class),
  JsonSubTypes.Type(name = "ReopenedEvent", value = BBPRReopenedEvent::class),
  JsonSubTypes.Type(name = "MergedEvent", value = BBPRMergedEvent::class),

  JsonSubTypes.Type(name = "AssignedEvent", value = BBPRAssignedEvent::class),
  JsonSubTypes.Type(name = "UnassignedEvent", value = BBPRUnassignedEvent::class),

  JsonSubTypes.Type(name = "LabeledEvent", value = BBPRLabeledEvent::class),
  JsonSubTypes.Type(name = "UnlabeledEvent", value = BBPRUnlabeledEvent::class),

  JsonSubTypes.Type(name = "ReviewRequestedEvent", value = BBPRReviewRequestedEvent::class),
  JsonSubTypes.Type(name = "ReviewRequestRemovedEvent", value = BBPRReviewUnrequestedEvent::class),

  JsonSubTypes.Type(name = "BaseRefChangedEvent", value = BBPRBaseRefChangedEvent::class),
  JsonSubTypes.Type(name = "BaseRefForcePushedEvent", value = BBPRBaseRefForcePushedEvent::class),

  JsonSubTypes.Type(name = "HeadRefDeletedEvent", value = BBPRHeadRefDeletedEvent::class),
  JsonSubTypes.Type(name = "HeadRefForcePushedEvent", value = BBPRHeadRefForcePushedEvent::class),
  JsonSubTypes.Type(name = "HeadRefRestoredEvent", value = BBPRHeadRefRestoredEvent::class)
)
interface BBPRTimelineItem {
  class Unknown : BBPRTimelineItem
}