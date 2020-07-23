// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.jetbrains.plugins.template.api.data.BBUser

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename", visible = false)
@JsonSubTypes(
  JsonSubTypes.Type(name = "User", value = BBUser::class),
  JsonSubTypes.Type(name = "Team", value = BBTeam::class)
)
interface BBPullRequestRequestedReviewer {
  val shortName: String
  val url: String
  val avatarUrl: String
  val name: String?
}