// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.pullrequest.avatars

import org.jetbrains.annotations.CalledInAwt
import javax.swing.Icon

interface BBAvatarIconsProvider {
  @CalledInAwt
  fun getIcon(avatarUrl: String?): Icon
}