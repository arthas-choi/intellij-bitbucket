// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename", visible = false)
@JsonSubTypes(
  JsonSubTypes.Type(name = "User", value = BBRepositoryOwnerName.User::class),
  JsonSubTypes.Type(name = "Organization", value = BBRepositoryOwnerName.Organization::class)
)
interface BBRepositoryOwnerName {
  val login: String

  class User(override val login: String) : BBRepositoryOwnerName
  class Organization(override val login: String) : BBRepositoryOwnerName
}
