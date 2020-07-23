// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api

import com.intellij.openapi.util.text.StringUtil

class BBRepositoryPath(val owner: String, val repository: String) {

  override fun toString() = "$owner/$repository"
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BBRepositoryPath) return false

    if (!owner.equals(other.owner, true)) return false
    if (!repository.equals(other.repository, true)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = StringUtil.stringHashCodeInsensitive(owner)
    result = 31 * result + StringUtil.stringHashCodeInsensitive(repository)
    return result
  }
}
