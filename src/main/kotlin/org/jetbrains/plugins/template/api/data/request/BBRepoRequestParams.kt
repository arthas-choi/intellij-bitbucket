// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.request

/**
 * Additional params for [`GET /user/repos`](https://developer.github.com/v3/repos/#list-your-repositories) request
 */
enum class Type(private val value: String) {
  ALL(""),
  OWNER("owner"),
  PUBLIC("public"),
  PRIVATE("private"),
  @Suppress("unused") // API
  MEMBER("member");

  companion object {
    val DEFAULT = ALL
  }

  override fun toString() = if (value.isEmpty()) value else "type=$value"
}

enum class Visibility(private val value: String) {
  ALL(""),
  PUBLIC("public"),
  PRIVATE("private");

  companion object {
    val DEFAULT = ALL
  }

  override fun toString() = if (value.isEmpty()) value else "visibility=$value"
}

class Affiliation private constructor(private val value: String) {
  companion object {
    val OWNER = Affiliation("owner")
    val COLLABORATOR = Affiliation("collaborator")
    @Suppress("unused") // API
    val ORG_MEMBER = Affiliation("organization_member")
    val DEFAULT = Affiliation("")

    fun combine(vararg affiliations: Affiliation): Affiliation {
      return Affiliation(affiliations.toSet().joinToString(",") { it.value })
    }
  }

  override fun toString() = if (value.isEmpty()) value else "affiliation=$value"
}