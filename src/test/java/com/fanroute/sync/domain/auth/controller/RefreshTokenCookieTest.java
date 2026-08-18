package com.fanroute.sync.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieTest {

  private final RefreshTokenCookie cookie = new RefreshTokenCookie(true, "Lax");

  @Test
  @DisplayName("Refresh Token Cookie를 제한된 인증 경로로 발급한다")
  void createsRefreshTokenCookie() {
    ResponseCookie responseCookie = cookie.create("refresh-token", 1209600);

    assertThat(responseCookie.getName()).isEqualTo("refreshToken");
    assertThat(responseCookie.getValue()).isEqualTo("refresh-token");
    assertThat(responseCookie.isHttpOnly()).isTrue();
    assertThat(responseCookie.isSecure()).isTrue();
    assertThat(responseCookie.getSameSite()).isEqualTo("Lax");
    assertThat(responseCookie.getPath()).isEqualTo("/api/v1/auth");
    assertThat(responseCookie.getMaxAge().getSeconds()).isEqualTo(1209600);
  }

  @Test
  @DisplayName("삭제 Cookie는 발급 Cookie와 동일한 Path를 사용한다")
  void clearsCookieWithIssuingPath() {
    ResponseCookie responseCookie = cookie.clear();

    assertThat(responseCookie.getValue()).isEmpty();
    assertThat(responseCookie.getPath()).isEqualTo("/api/v1/auth");
    assertThat(responseCookie.getMaxAge().isZero()).isTrue();
  }
}
