// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data;

import org.jetbrains.annotations.Nullable;

public class BitbucketPullRequestDetailed extends BitbucketPullRequest {
  private Boolean merged;
  private Boolean mergeable;
  private Boolean rebaseable;
  private String mergeableState;
  private BitbucketUser mergedBy;

  private Integer comments;
  private Integer reviewComments;
  private Boolean maintainerCanModify;
  private Integer commits;
  private Integer additions;
  private Integer deletions;
  private Integer changedFiles;

  public boolean getMerged() {
    return merged;
  }

  @Nullable
  public Boolean getMergeable() {
    return mergeable;
  }

  @Nullable
  public Boolean getRebaseable() {
    return rebaseable;
  }
}
