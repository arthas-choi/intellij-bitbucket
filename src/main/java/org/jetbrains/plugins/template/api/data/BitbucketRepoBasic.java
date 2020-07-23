// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.template.api.BBRepositoryPath;


import java.util.Objects;

@SuppressWarnings("UnusedDeclaration")
public class BitbucketRepoBasic {
  private Long id;
  //private String nodeId;
  private String name;
  private String fullName;
  private BitbucketUser owner;
  @JsonProperty("private")
  private Boolean isPrivate;
  private String htmlUrl;
  private String description;
  @JsonProperty("fork")
  private Boolean isFork;

  private String url;
  //urls

  @NotNull
  public String getName() {
    return name;
  }

  @NotNull
  public String getDescription() {
    return StringUtil.notNullize(description);
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public boolean isFork() {
    return isFork;
  }

  @NotNull
  public String getUrl() {
    return url;
  }

  @NotNull
  public String getHtmlUrl() {
    return htmlUrl;
  }

  @NotNull
  public BitbucketUser getOwner() {
    return owner;
  }


  @NotNull
  public String getUserName() {
    return getOwner().getLogin();
  }

  @NotNull
  public String getFullName() {
    return getUserName() + "/" + getName();
  }

  @NotNull
  public BBRepositoryPath getFullPath() {
    return new BBRepositoryPath(getUserName(), getName());
  }

  @Override
  public String toString() {
    return "GithubRepo{" +
           "id=" + id +
           ", name='" + name + '\'' +
           '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BitbucketRepoBasic)) return false;
    BitbucketRepoBasic basic = (BitbucketRepoBasic)o;
    return id.equals(basic.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
