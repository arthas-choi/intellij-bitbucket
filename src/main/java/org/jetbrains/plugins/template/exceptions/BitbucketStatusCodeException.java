/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.template.exceptions;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.template.api.data.BitbucketErrorMessage;


/**
 * @author Aleksey Pivovarov
 */
public class BitbucketStatusCodeException extends BitbucketConfusingException {
  private final int myStatusCode;
  private final BitbucketErrorMessage myError;

  public BitbucketStatusCodeException(String message, int statusCode) {
    this(message, null, statusCode);
  }

  public BitbucketStatusCodeException(String message, BitbucketErrorMessage error, int statusCode) {
    super(message);
    myStatusCode = statusCode;
    myError = error;
  }

  public int getStatusCode() {
    return myStatusCode;
  }

  @Nullable
  public BitbucketErrorMessage getError() {
    return myError;
  }
}
