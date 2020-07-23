// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data;

public class BitbucketPermissions {
  private Boolean admin;
  private Boolean pull;
  private Boolean push;

  public boolean isAdmin() {
    return admin;
  }

  public boolean isPull() {
    return pull;
  }

  public boolean isPush() {
    return push;
  }
}