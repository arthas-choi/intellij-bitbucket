// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.URLUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.template.exceptions.BitbucketParseException;
import org.jetbrains.plugins.template.util.BitbucketUrlUtil;


import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Github server reference allowing to specify custom port and path to instance
 */
@Tag("server")
public class BitbucketServerPath {
  public static final String DEFAULT_HOST = "github.com";
  public static final BitbucketServerPath DEFAULT_SERVER = new BitbucketServerPath(DEFAULT_HOST);
  private static final String API_PREFIX = "api.";
  private static final String API_SUFFIX = "/api";
  private static final String ENTERPRISE_API_V3_SUFFIX = "/v3";
  private static final String GRAPHQL_SUFFIX = "/graphql";

  @Attribute("useHttp")
  @Nullable private final Boolean myUseHttp;
  @Attribute("host")
  @NotNull private final String myHost;
  @Attribute("port")
  @Nullable private final Integer myPort;
  @Attribute("suffix")
  @Nullable private final String mySuffix;

  public BitbucketServerPath() {
    this(null, "", null, null);
  }

  public BitbucketServerPath(@NonNls @NotNull String host) {
    this(null, host, null, null);
  }

  public BitbucketServerPath(@Nullable Boolean useHttp,
                             @NonNls @NotNull String host,
                             @Nullable Integer port,
                             @NonNls @Nullable String suffix) {
    myUseHttp = useHttp;
    myHost = StringUtil.toLowerCase(host);
    myPort = port;
    mySuffix = suffix;
  }

  @NotNull
  public String getSchema() {
    return (myUseHttp == null || !myUseHttp) ? "https" : "http";
  }

  @NotNull
  public String getHost() {
    return myHost;
  }

  @Nullable
  public Integer getPort() {
    return myPort;
  }

  @Nullable
  public String getSuffix() {
    return mySuffix;
  }

  public boolean matches(@NotNull String gitRemoteUrl) {
    String url = BitbucketUrlUtil.removePort(BitbucketUrlUtil.removeProtocolPrefix(gitRemoteUrl));
    return StringUtil.startsWithIgnoreCase(url, myHost + StringUtil.notNullize(mySuffix));
  }

  // 1 - schema, 2 - host, 4 - port, 5 - path
  private final static Pattern URL_REGEX = Pattern.compile("^(https?://)?([^/?:]+)(:(\\d+))?((/[^/?#]+)*)?/?",
                                                           Pattern.CASE_INSENSITIVE);

  @NotNull
  public static BitbucketServerPath from(@NotNull String uri) throws BitbucketParseException {
    Matcher matcher = URL_REGEX.matcher(uri);

    if (!matcher.matches()) throw new BitbucketParseException("Not a valid URL");
    String schema = matcher.group(1);
    Boolean httpSchema = (schema == null || schema.isEmpty()) ? null : schema.equalsIgnoreCase("http://");
    String host = matcher.group(2);
    if (host == null) throw new BitbucketParseException("Empty host");

    Integer port;
    String portGroup = matcher.group(4);
    if (portGroup == null) {
      port = null;
    }
    else {
      try {
        port = Integer.parseInt(portGroup);
      }
      catch (NumberFormatException e) {
        throw new BitbucketParseException("Invalid port format");
      }
    }

    String path = StringUtil.nullize(matcher.group(5));

    return new BitbucketServerPath(httpSchema, host, port, path);
  }

  @NotNull
  public String toUrl() {
    return getSchemaUrlPart() + myHost + getPortUrlPart() + StringUtil.notNullize(mySuffix);
  }

  @NotNull
  public String toApiUrl() {
    StringBuilder builder = new StringBuilder(getSchemaUrlPart());
    if (isGithubDotCom()) {
      builder.append(API_PREFIX).append(myHost).append(getPortUrlPart()).append(StringUtil.notNullize(mySuffix));
    }
    else {
      builder.append(myHost).append(getPortUrlPart()).append(StringUtil.notNullize(mySuffix)).append(API_SUFFIX)
        .append(ENTERPRISE_API_V3_SUFFIX);
    }
    return builder.toString();
  }

  @NotNull
  public String toGraphQLUrl() {
    StringBuilder builder = new StringBuilder(getSchemaUrlPart());
    if (isGithubDotCom()) {
      builder.append(API_PREFIX).append(myHost).append(getPortUrlPart()).append(StringUtil.notNullize(mySuffix)).append(GRAPHQL_SUFFIX);
    }
    else {
      builder.append(myHost).append(getPortUrlPart()).append(StringUtil.notNullize(mySuffix)).append(API_SUFFIX).append(GRAPHQL_SUFFIX);
    }
    return builder.toString();
  }

  public boolean isGithubDotCom() {
    return myHost.equalsIgnoreCase(DEFAULT_HOST);
  }

  public String toString() {
    String schema = myUseHttp != null ? getSchemaUrlPart() : "";
    return schema + myHost + getPortUrlPart() + StringUtil.notNullize(mySuffix);
  }

  @NotNull
  private String getPortUrlPart() {
    return myPort != null ? (":" + myPort.toString()) : "";
  }

  @NotNull
  private String getSchemaUrlPart() {
    return getSchema() + URLUtil.SCHEME_SEPARATOR;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BitbucketServerPath)) return false;
    BitbucketServerPath path = (BitbucketServerPath)o;
    return Objects.equals(myUseHttp, path.myUseHttp) &&
           Objects.equals(myHost, path.myHost) &&
           Objects.equals(myPort, path.myPort) &&
           Objects.equals(mySuffix, path.mySuffix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myUseHttp, myHost, myPort, mySuffix);
  }
}
