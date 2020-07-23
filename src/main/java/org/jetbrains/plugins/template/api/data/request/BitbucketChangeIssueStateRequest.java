// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.request;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class BitbucketChangeIssueStateRequest {
  @NotNull private final String state;

  public BitbucketChangeIssueStateRequest(@NotNull String state) {
    this.state = state;
  }
}
