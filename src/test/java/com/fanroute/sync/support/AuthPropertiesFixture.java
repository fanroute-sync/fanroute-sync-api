package com.fanroute.sync.support;

import java.net.URI;
import java.time.Duration;

import com.fanroute.sync.domain.auth.config.AuthProperties;

public final class AuthPropertiesFixture {

  public static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(1);
  public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
  public static final String JWT_ISSUER = "https://api.test.fanroute.com";
  // Base64, 32바이트 이상 (HS256 키 길이 요건)
  public static final String JWT_SECRET =
      "dGVzdC1vbmx5LWtleS10aGF0LWlzLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=";

  private AuthPropertiesFixture() {
  }

  public static AuthProperties defaultProperties() {
    return new AuthProperties(
        new AuthProperties.Google(
            "client-id",
            "client-secret",
            URI.create("http://localhost/callback"),
            "https://accounts.google.com",
            URI.create("https://www.googleapis.com/oauth2/v3/certs")),
        new AuthProperties.Jwt(JWT_ISSUER, JWT_SECRET, ACCESS_TOKEN_TTL),
        new AuthProperties.Refresh(REFRESH_TOKEN_TTL));
  }
}
