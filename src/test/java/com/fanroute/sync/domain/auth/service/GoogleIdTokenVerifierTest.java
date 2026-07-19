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
  @DisplayName("검증된 Google ID Token에서 사용자 식별자를 추출한다")
  void extractsSubject() {
    Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
        Map.of("alg", "RS256"), Map.of("sub", "google-sub"));
    when(decoder.decode("token")).thenReturn(jwt);

    String subject = new GoogleIdTokenVerifier(decoder).verifyAndExtractSubject("token");

    assertThat(subject).isEqualTo("google-sub");
  }

  @Test
  @DisplayName("유효하지 않은 Google ID Token을 인증 실패로 변환한다")
  void rejectsInvalidIdToken() {
    when(decoder.decode("invalid")).thenThrow(new JwtException("invalid signature"));

    assertThatThrownBy(() -> new GoogleIdTokenVerifier(decoder)
        .verifyAndExtractSubject("invalid"))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.GOOGLE_ID_TOKEN_INVALID));
  }
}