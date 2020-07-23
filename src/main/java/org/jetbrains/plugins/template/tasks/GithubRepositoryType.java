// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.tasks;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.TaskState;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.tasks.impl.BaseRepositoryType;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;

/**
 * @author Dennis.Ushakov
 */
public class GithubRepositoryType extends BaseRepositoryType<BitbucketRepository> {

  @NotNull
  @Override
  public String getName() {
    return "GitHub";
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Vcs.Vendors.Github;
  }

  @NotNull
  @Override
  public TaskRepository createRepository() {
    return new BitbucketRepository(this);
  }

  @Override
  public Class<BitbucketRepository> getRepositoryClass() {
    return org.jetbrains.plugins.template.tasks.BitbucketRepository.class;
  }

  @NotNull
  @Override
  public TaskRepositoryEditor createEditor(BitbucketRepository repository,
                                           Project project,
                                           Consumer<BitbucketRepository> changeListener) {
    return new GithubRepositoryEditor(project, repository, changeListener);
  }

  @Override
  public EnumSet<TaskState> getPossibleTaskStates() {
    return EnumSet.of(TaskState.OPEN, TaskState.RESOLVED);
  }

}
