package com.fanroute.sync.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fanroute.sync.domain.auth.controller.RefreshTokenCookie;

class RefreshTokenCookieClearerTest {

  @Test
  @DisplayName("Refresh Token Cookie 삭제 값을 RefreshTokenCookie에 위임한다")
  void delegatesClearToRefreshTokenCookie() {
    RefreshTokenCookie refreshTokenCookie = new RefreshTokenCookie(true, "Lax");
    RefreshTokenCookieClearer clearer = new RefreshTokenCookieClearer(refreshTokenCookie);

    String result = clearer.clear();

    assertThat(result).isEqualTo(refreshTokenCookie.clear().toString());
  }
}
