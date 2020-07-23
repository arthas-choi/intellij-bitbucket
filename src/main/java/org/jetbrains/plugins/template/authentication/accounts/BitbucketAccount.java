// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.authentication.accounts;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.template.api.BitbucketServerPath;


import java.util.Objects;
import java.util.UUID;

@Tag("account")
public class BitbucketAccount {
  @Attribute("id")
  @NotNull private final String myId;
  @Attribute("name")
  @NotNull private String myName;
  @Property(style = Property.Style.ATTRIBUTE, surroundWithTag = false)
  @NotNull private final BitbucketServerPath myServer;

  // serialization
  @SuppressWarnings("unused")
  private BitbucketAccount() {
    myId = "";
    myName = "";
    myServer = new BitbucketServerPath();
  }

  BitbucketAccount(@NotNull String name, @NotNull BitbucketServerPath server) {
    myId = UUID.randomUUID().toString();
    myName = name;
    myServer = server;
  }

  @Override
  public String toString() {
    return myServer + "/" + myName;
  }

  @NotNull
  String getId() {
    return myId;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @Transient
  public void setName(@NotNull String name) {
    myName = name;
  }

  @NotNull
  public BitbucketServerPath getServer() {
    return myServer;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BitbucketAccount)) return false;
    BitbucketAccount account = (BitbucketAccount)o;
    return Objects.equals(myId, account.myId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myId);
  }
}