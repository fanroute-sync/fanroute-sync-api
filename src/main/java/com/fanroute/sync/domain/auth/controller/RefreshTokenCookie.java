package com.fanroute.sync.domain.auth.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookie {

  public static final String NAME = "refreshToken";
  private static final String PATH = "/api/v1/auth";

  private final boolean secure;
  private final String sameSite;

  public RefreshTokenCookie(
      @Value("${auth.refresh.cookie.secure:true}") boolean secure,
      @Value("${auth.refresh.cookie.same-site:Lax}") String sameSite) {
    this.secure = secure;
    this.sameSite = sameSite;
  }

  public ResponseCookie create(String token, long maxAgeSeconds) {
    return base(token).maxAge(Duration.ofSeconds(maxAgeSeconds)).build();
  }

  public ResponseCookie clear() {
    return base("").maxAge(Duration.ZERO).build();
  }

  private ResponseCookie.ResponseCookieBuilder base(String value) {
    return ResponseCookie.from(NAME, value)
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite)
        .path(PATH);
  }
}
