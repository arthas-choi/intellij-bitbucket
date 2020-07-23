// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.tasks;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.PasswordUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.template.api.BitbucketApiRequestExecutor;
import org.jetbrains.plugins.template.api.BitbucketApiRequests;
import org.jetbrains.plugins.template.api.BitbucketServerPath;
import org.jetbrains.plugins.template.api.data.BitbucketIssue;
import org.jetbrains.plugins.template.api.data.BitbucketIssueBase;
import org.jetbrains.plugins.template.api.data.BitbucketIssueCommentWithHtml;
import org.jetbrains.plugins.template.api.data.BitbucketIssueState;
import org.jetbrains.plugins.template.api.util.BitbucketApiPagesLoader;
import org.jetbrains.plugins.template.exceptions.BitbucketAuthenticationException;
import org.jetbrains.plugins.template.exceptions.BitbucketJsonException;
import org.jetbrains.plugins.template.exceptions.BitbucketRateLimitExceededException;
import org.jetbrains.plugins.template.exceptions.BitbucketStatusCodeException;
import org.jetbrains.plugins.template.issue.BitbucketIssuesLoadingHelper;


import javax.swing.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dennis.Ushakov
 */
@Tag("GitHub")
public class BitbucketRepository extends BaseRepository {
  private static final Logger LOG = Logger.getInstance(BitbucketRepository.class);

  private Pattern myPattern = Pattern.compile("($^)");
  @NotNull private String myRepoAuthor = "";
  @NotNull private String myRepoName = "";
  @NotNull private String myUser = "";
  private boolean myAssignedIssuesOnly = false;

  @SuppressWarnings({"UnusedDeclaration"})
  public BitbucketRepository() {
  }

  public BitbucketRepository(BitbucketRepository other) {
    super(other);
    setRepoName(other.myRepoName);
    setRepoAuthor(other.myRepoAuthor);
    setAssignedIssuesOnly(other.myAssignedIssuesOnly);
  }

  public BitbucketRepository(GithubRepositoryType type) {
    super(type);
    setUrl("https://" + BitbucketServerPath.DEFAULT_HOST);
  }

  @NotNull
  @Override
  public CancellableConnection createCancellableConnection() {
    return new CancellableConnection() {
      private final BitbucketApiRequestExecutor myExecutor = getExecutor();
      private final ProgressIndicator myIndicator = new EmptyProgressIndicator();

      @Override
      protected void doTest() throws Exception {
        try {
          myExecutor.execute(myIndicator, BitbucketApiRequests.Repos.get(getServer(), getRepoAuthor(), getRepoName()));
        }
        catch (ProcessCanceledException ignore) {
        }
      }

      @Override
      public void cancel() {
        myIndicator.cancel();
      }
    };
  }

  @Override
  public boolean isConfigured() {
    return super.isConfigured() &&
           !StringUtil.isEmptyOrSpaces(getRepoAuthor()) &&
           !StringUtil.isEmptyOrSpaces(getRepoName()) &&
           !StringUtil.isEmptyOrSpaces(getPassword());
  }

  @Override
  public String getPresentableName() {
    final String name = super.getPresentableName();
    return name +
           (!StringUtil.isEmpty(getRepoAuthor()) ? "/" + getRepoAuthor() : "") +
           (!StringUtil.isEmpty(getRepoName()) ? "/" + getRepoName() : "");
  }

  @Override
  public Task[] getIssues(@Nullable String query, int offset, int limit, boolean withClosed) throws Exception {
    try {
      return getIssues(query, offset + limit, withClosed);
    }
    catch (BitbucketRateLimitExceededException e) {
      return Task.EMPTY_ARRAY;
    }
    catch (BitbucketAuthenticationException | BitbucketStatusCodeException e) {
      throw new Exception(e.getMessage(), e); // Wrap to show error message
    }
    catch (BitbucketJsonException e) {
      throw new Exception("Bad response format", e);
    }
  }

  @Override
  public Task[] getIssues(@Nullable String query, int offset, int limit, boolean withClosed, @NotNull ProgressIndicator cancelled)
    throws Exception {
    return getIssues(query, offset, limit, withClosed);
  }

  private Task @NotNull [] getIssues(@Nullable String query, int max, boolean withClosed) throws Exception {
    BitbucketApiRequestExecutor executor = getExecutor();
    ProgressIndicator indicator = getProgressIndicator();
    BitbucketServerPath server = getServer();

    String assigned = null;
    if (myAssignedIssuesOnly) {
      if (StringUtil.isEmptyOrSpaces(myUser)) {
        myUser = executor.execute(indicator, BitbucketApiRequests.CurrentUser.get(server)).getLogin();
      }
      assigned = myUser;
    }

    List<? extends BitbucketIssueBase> issues;
    if (StringUtil.isEmptyOrSpaces(query)) {
      // search queries have way smaller request number limit
      issues = BitbucketIssuesLoadingHelper.load(executor, indicator, server, getRepoAuthor(), getRepoName(), withClosed, max, assigned);
    }
    else {
      issues = BitbucketIssuesLoadingHelper.search(executor, indicator, server, getRepoAuthor(), getRepoName(), withClosed, assigned, query);
    }
    List<Task> tasks = new ArrayList<>();

    for (BitbucketIssueBase issue : issues) {
      List<BitbucketIssueCommentWithHtml> comments = BitbucketApiPagesLoader
        .loadAll(executor, indicator, BitbucketApiRequests.Repos.Issues.Comments.pages(issue.getCommentsUrl()));
      tasks.add(createTask(issue, comments));
    }

    return tasks.toArray(Task.EMPTY_ARRAY);
  }

  @NotNull
  private Task createTask(@NotNull BitbucketIssueBase issue, @NotNull List<BitbucketIssueCommentWithHtml> comments) {
    return new Task() {
      @NotNull private final String myRepoName = getRepoName();
      private final Comment @NotNull [] myComments =
        ContainerUtil.map2Array(comments, Comment.class, comment -> new GithubComment(comment.getCreatedAt(),
                                                                                      comment.getUser().getLogin(),
                                                                                      comment.getBodyHtml(),
                                                                                      comment.getUser().getAvatarUrl(),
                                                                                      comment.getUser().getHtmlUrl()));

      @Override
      public boolean isIssue() {
        return true;
      }

      @Override
      public String getIssueUrl() {
        return issue.getHtmlUrl();
      }

      @NotNull
      @Override
      public String getId() {
        return myRepoName + "-" + issue.getNumber();
      }

      @NotNull
      @Override
      public String getSummary() {
        return issue.getTitle();
      }

      @Override
      public String getDescription() {
        return issue.getBody();
      }

      @Override
      public Comment @NotNull [] getComments() {
        return myComments;
      }

      @NotNull
      @Override
      public Icon getIcon() {
        return AllIcons.Vcs.Vendors.Github;
      }

      @NotNull
      @Override
      public TaskType getType() {
        return TaskType.BUG;
      }

      @Override
      public Date getUpdated() {
        return issue.getUpdatedAt();
      }

      @Override
      public Date getCreated() {
        return issue.getCreatedAt();
      }

      @Override
      public boolean isClosed() {
        return issue.getState() == BitbucketIssueState.closed;
      }

      @Override
      public TaskRepository getRepository() {
        return BitbucketRepository.this;
      }

      @Override
      public String getPresentableName() {
        return getId() + ": " + getSummary();
      }
    };
  }

  @Override
  @Nullable
  public String extractId(@NotNull String taskName) {
    Matcher matcher = myPattern.matcher(taskName);
    return matcher.find() ? matcher.group(1) : null;
  }

  @Nullable
  @Override
  public Task findTask(@NotNull String id) throws Exception {
    final int index = id.lastIndexOf("-");
    if (index < 0) {
      return null;
    }
    final String numericId = id.substring(index + 1);
    BitbucketApiRequestExecutor executor = getExecutor();
    ProgressIndicator indicator = getProgressIndicator();
    BitbucketIssue issue = executor.execute(indicator,
                                         BitbucketApiRequests.Repos.Issues.get(getServer(), getRepoAuthor(), getRepoName(), numericId));
    if (issue == null) return null;
    List<BitbucketIssueCommentWithHtml> comments = BitbucketApiPagesLoader
      .loadAll(executor, indicator, BitbucketApiRequests.Repos.Issues.Comments.pages(issue.getCommentsUrl()));
    return createTask(issue, comments);
  }

  @Override
  public void setTaskState(@NotNull Task task, @NotNull TaskState state) throws Exception {
    boolean isOpen;
    switch (state) {
      case OPEN:
        isOpen = true;
        break;
      case RESOLVED:
        isOpen = false;
        break;
      default:
        throw new IllegalStateException("Unknown state: " + state);
    }
    BitbucketApiRequestExecutor executor = getExecutor();
    BitbucketServerPath server = getServer();
    String repoAuthor = getRepoAuthor();
    String repoName = getRepoName();

    ProgressIndicator indicator = getProgressIndicator();
    executor.execute(indicator,
                     BitbucketApiRequests.Repos.Issues.updateState(server, repoAuthor, repoName, task.getNumber(), isOpen));
  }

  @NotNull
  @Override
  public BaseRepository clone() {
    return new BitbucketRepository(this);
  }

  @NotNull
  public String getRepoName() {
    return myRepoName;
  }

  public void setRepoName(@NotNull String repoName) {
    myRepoName = repoName;
    myPattern = Pattern.compile("(" + StringUtil.escapeToRegexp(repoName) + "\\-\\d+)");
  }

  @NotNull
  public String getRepoAuthor() {
    return myRepoAuthor;
  }

  public void setRepoAuthor(@NotNull String repoAuthor) {
    myRepoAuthor = repoAuthor;
  }

  @NotNull
  public String getUser() {
    return myUser;
  }

  public void setUser(@NotNull String user) {
    myUser = user;
  }

  /**
   * Stores access token
   */
  @Override
  public void setPassword(String password) {
    super.setPassword(password);
    setUser("");
  }

  public boolean isAssignedIssuesOnly() {
    return myAssignedIssuesOnly;
  }

  public void setAssignedIssuesOnly(boolean value) {
    myAssignedIssuesOnly = value;
  }

  @Deprecated
  @Tag("token")
  public String getEncodedToken() {
    return null;
  }

  @Deprecated
  @SuppressWarnings("unused")
  public void setEncodedToken(String password) {
    try {
      setPassword(PasswordUtil.decodePassword(password));
    }
    catch (NumberFormatException e) {
      LOG.warn("Can't decode token", e);
    }
  }

  @Override
  @NotNull
  protected CredentialAttributes getAttributes() {
    String serviceName = CredentialAttributesKt.generateServiceName("Tasks", getRepositoryType().getName() + " " + getPresentableName());
    return new CredentialAttributes(serviceName, "GitHub OAuth token");
  }

  @NotNull
  private BitbucketApiRequestExecutor getExecutor() {
    return BitbucketApiRequestExecutor.Factory.getInstance().create(getPassword(), myUseProxy);
  }

  @NotNull
  private static ProgressIndicator getProgressIndicator() {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator == null) indicator = new EmptyProgressIndicator();
    return indicator;
  }

  @NotNull
  private BitbucketServerPath getServer() {
    return BitbucketServerPath.from(getUrl());
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) return false;
    if (!(o instanceof BitbucketRepository)) return false;

    BitbucketRepository that = (BitbucketRepository)o;
    if (!Comparing.equal(getRepoAuthor(), that.getRepoAuthor())) return false;
    if (!Comparing.equal(getRepoName(), that.getRepoName())) return false;
    if (!Comparing.equal(isAssignedIssuesOnly(), that.isAssignedIssuesOnly())) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return StringUtil.stringHashCode(getRepoName()) +
           31 * StringUtil.stringHashCode(getRepoAuthor());
  }

  @Override
  protected int getFeatures() {
    return super.getFeatures() | STATE_UPDATING;
  }
}
