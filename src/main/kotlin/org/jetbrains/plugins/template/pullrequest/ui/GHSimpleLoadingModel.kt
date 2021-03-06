// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.ui

abstract class GHSimpleLoadingModel<T> : GHEventDispatcherLoadingModel() {
  abstract val result: T?

  override val resultAvailable: Boolean
    get() = result != null
}