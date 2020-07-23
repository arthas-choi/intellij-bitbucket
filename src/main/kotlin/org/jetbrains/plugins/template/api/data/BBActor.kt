// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename", visible = false,
              defaultImpl = BBActor::class)
@JsonSubTypes(
  JsonSubTypes.Type(name = "User", value = BBUser::class),
  JsonSubTypes.Type(name = "Bot", value = BBBot::class),
  JsonSubTypes.Type(name = "Mannequin", value = BBMannequin::class),
  JsonSubTypes.Type(name = "Organization", value = BBOrganization::class)
)
interface BBActor {
  val login: String
  val url: String
  val avatarUrl: String
}