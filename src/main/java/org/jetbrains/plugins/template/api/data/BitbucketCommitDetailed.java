// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public class BitbucketCommitDetailed extends BitbucketCommit {
  private CommitStats stats;
  private List<BitbucketFile> files;

  @NotNull
  public CommitStats getStats() {
    return stats;
  }

  @NotNull
  public List<BitbucketFile> getFiles() {
    return files;
  }

  public static class CommitStats {
    private Integer additions;
    private Integer deletions;
    private Integer total;

    public int getAdditions() {
      return additions;
    }

    public int getDeletions() {
      return deletions;
    }

    public int getTotal() {
      return total;
    }
  }
}
