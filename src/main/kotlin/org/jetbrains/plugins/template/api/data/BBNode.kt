// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data

open class BBNode(val id: String) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BBNode) return false

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}