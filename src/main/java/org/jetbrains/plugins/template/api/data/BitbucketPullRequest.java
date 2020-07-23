// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data;

import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnusedDeclaration")
public class BitbucketPullRequest {
  private String url;
  private Long id;

  //non-api urls
  private String htmlUrl;
  private String diffUrl;
  private String patchUrl;

  private Long number;
  private BitbucketIssueState state;
  private Boolean locked;
  private String activeLockReason;
  private String title;
  private BitbucketUser user;
  private String body;

  private Date updatedAt;
  private Date closedAt;
  private Date mergedAt;
  private Date createdAt;
  private String mergeCommitSha;
  private List<BitbucketUser> assignees;
  private List<BitbucketUser> requestedReviewers;
  //requestedTeams
  private List<BitbucketIssueLabel> labels;
  //milestone

  private Tag head;
  private Tag base;
  private Links _links;
  private String authorAssociation;

  @NotNull
  public String getUrl() {
    return url;
  }

  @NotNull
  public String getHtmlUrl() {
    return htmlUrl;
  }

  @NotNull
  public String getDiffUrl() {
    return diffUrl;
  }

  @NotNull
  public String getPatchUrl() {
    return patchUrl;
  }

  public long getNumber() {
    return number;
  }

  @NotNull
  public BitbucketIssueState getState() {
    return state;
  }

  @NotNull
  public String getTitle() {
    return title;
  }

  @NotNull
  public String getBody() {
    return body;
  }

  @NotNull
  public Date getCreatedAt() {
    return createdAt;
  }

  @NotNull
  public Date getUpdatedAt() {
    return updatedAt;
  }

  @Nullable
  public Date getClosedAt() {
    return closedAt;
  }

  @Nullable
  public Date getMergedAt() {
    return mergedAt;
  }

  @NotNull
  public BitbucketUser getUser() {
    return ObjectUtils.notNull(user, BitbucketUser.UNKNOWN);
  }

  @NotNull
  public List<BitbucketUser> getAssignees() {
    return assignees;
  }

  @NotNull
  public List<BitbucketUser> getRequestedReviewers() {
    return requestedReviewers;
  }

  @Nullable
  public List<BitbucketIssueLabel> getLabels() {
    return labels;
  }

  @NotNull
  public Links getLinks() {
    return _links;
  }

  @NotNull
  public Tag getHead() {
    return head;
  }

  @NotNull
  public Tag getBase() {
    return base;
  }


  public static class Tag {
    private String label;
    private String ref;
    private String sha;

    private BitbucketRepo repo;
    private BitbucketUser user;

    @NotNull
    public String getLabel() {
      return label;
    }

    @NotNull
    public String getRef() {
      return ref;
    }

    @NotNull
    public String getSha() {
      return sha;
    }

    @Nullable
    public BitbucketRepo getRepo() {
      return repo;
    }

    @Nullable
    public BitbucketUser getUser() {
      return user;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Tag)) return false;
      Tag tag = (Tag)o;
      return Objects.equals(label, tag.label) &&
             Objects.equals(ref, tag.ref) &&
             Objects.equals(sha, tag.sha) &&
             Objects.equals(repo, tag.repo) &&
             Objects.equals(user, tag.user);
    }

    @Override
    public int hashCode() {
      return Objects.hash(label, ref, sha, repo, user);
    }
  }


  public static class Links {
    private BitbucketLink self;
    private BitbucketLink html;
    private BitbucketLink issue;
    private BitbucketLink comments;
    private BitbucketLink reviewComments;
    private BitbucketLink reviewComment;
    private BitbucketLink commits;
    private BitbucketLink statuses;
  }
}
