// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.request;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.template.api.data.BitbucketPullRequestMergeMethod;


@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class BitbucketPullRequestMergeRequest {
  @NotNull private final String commitTitle;
  @NotNull private final String commitMessage;
  @NotNull private final String sha;
  @NotNull private final BitbucketPullRequestMergeMethod mergeMethod;

  public BitbucketPullRequestMergeRequest(@NotNull String commitTitle,
                                          @NotNull String commitMessage,
                                          @NotNull String sha,
                                          @NotNull BitbucketPullRequestMergeMethod mergeMethod) {
    if (mergeMethod != BitbucketPullRequestMergeMethod.merge && mergeMethod != BitbucketPullRequestMergeMethod.squash) {
      throw new IllegalArgumentException("Invalid merge method");
    }

    this.commitTitle = commitTitle;
    this.commitMessage = commitMessage;
    this.sha = sha;
    this.mergeMethod = mergeMethod;
  }
}
