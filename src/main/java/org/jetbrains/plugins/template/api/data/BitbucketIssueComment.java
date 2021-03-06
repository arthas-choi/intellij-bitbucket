// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

@SuppressWarnings("UnusedDeclaration")
public class BitbucketIssueComment {
  private Long id;

  private String url;
  private String htmlUrl;
  private String body;

  private Date createdAt;
  private Date updatedAt;

  private BitbucketUser user;

  public long getId() {
    return id;
  }

  @NotNull
  public String getHtmlUrl() {
    return htmlUrl;
  }

  @NotNull
  public Date getCreatedAt() {
    return createdAt;
  }

  @NotNull
  public Date getUpdatedAt() {
    return updatedAt;
  }

  @NotNull
  public BitbucketUser getUser() {
    return user;
  }
}
