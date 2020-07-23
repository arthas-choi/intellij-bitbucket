// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnusedDeclaration")
public class BitbucketRepoDetailed extends BitbucketRepoWithPermissions {
  private Boolean allowSquashMerge;
  private Boolean allowMergeCommit;
  private Boolean allowRebaseMerge;
  private BitbucketOrg organization;
  private BitbucketRepo parent;
  private BitbucketRepo source;
  private Integer networkCount;
  private Integer subscribersCount;

  public boolean getAllowSquashMerge() {
    return allowSquashMerge != null ? allowSquashMerge : false;
  }

  public boolean getAllowMergeCommit() {
    return allowMergeCommit != null ? allowMergeCommit : false;
  }

  public boolean getAllowRebaseMerge() {
    return allowRebaseMerge != null ? allowRebaseMerge : false;
  }

  @Nullable
  public BitbucketRepo getParent() {
    return parent;
  }

  @Nullable
  public BitbucketRepo getSource() {
    return source;
  }
}
