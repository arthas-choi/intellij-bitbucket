// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.template.authentication.accounts.BitbucketAccount;


public class BitbucketMissingTokenException extends BitbucketAuthenticationException {
  public BitbucketMissingTokenException(@NotNull String message) {
    super(message);
  }

  public BitbucketMissingTokenException(@NotNull BitbucketAccount account) {
    this("Missing access token for account " + account);
  }
}
