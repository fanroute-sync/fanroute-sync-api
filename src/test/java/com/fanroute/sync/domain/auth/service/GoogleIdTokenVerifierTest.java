package com.fanroute.sync.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class GoogleIdTokenVerifierTest {

  @Mock
  private JwtDecoder decoder;

  @Test
  @DisplayName("검증된 Google ID Token에서 사용자 식별자와 이메일을 추출한다")
  void extractsUserInfo() {
    Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
        Map.of("alg", "RS256"), Map.of(
            "sub", "google-sub",
            "email", "user@example.com",
            "email_verified", true));
    when(decoder.decode("token")).thenReturn(jwt);

    GoogleIdTokenVerifier.GoogleUserInfo userInfo =
        new GoogleIdTokenVerifier(decoder).verifyAndExtractUserInfo("token");

    assertThat(userInfo.subject()).isEqualTo("google-sub");
    assertThat(userInfo.email()).isEqualTo("user@example.com");
  }

  @Test
  @DisplayName("Google 이메일이 검증되지 않았으면 인증에 실패한다")
  void rejectsUnverifiedEmail() {
    Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
        Map.of("alg", "RS256"), Map.of(
            "sub", "google-sub",
            "email", "user@example.com",
            "email_verified", false));
    when(decoder.decode("token")).thenReturn(jwt);

    assertThatThrownBy(() -> new GoogleIdTokenVerifier(decoder)
        .verifyAndExtractUserInfo("token"))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.GOOGLE_ID_TOKEN_INVALID));
  }

  @Test
  @DisplayName("유효하지 않은 Google ID Token을 인증 실패로 변환한다")
  void rejectsInvalidIdToken() {
    when(decoder.decode("invalid")).thenThrow(new JwtException("invalid signature"));

    assertThatThrownBy(() -> new GoogleIdTokenVerifier(decoder)
        .verifyAndExtractUserInfo("invalid"))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.GOOGLE_ID_TOKEN_INVALID));
  }
}
