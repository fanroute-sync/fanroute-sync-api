package com.fanroute.sync.domain.auth.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;

/**
 * 검증된 Google ID Token에서 변경되지 않는 계정 식별자인 {@code sub}를 추출합니다.
 */
@Component
public class GoogleIdTokenVerifier {

  private final JwtDecoder decoder;

  // RequiredArgsConstructor를 사용할 때, 명시적으로 지정이 어려움으로
  public GoogleIdTokenVerifier(
      @Qualifier("googleIdTokenDecoder") JwtDecoder decoder) {
    this.decoder = decoder;
  }

  /**
   * 서명과 표준 claim 검증에 성공한 토큰만 사용자 식별자로 변환합니다.
   */
  public String verifyAndExtractSubject(String idToken) {
    try {
      Jwt jwt = decoder.decode(idToken);
      String subject = jwt.getSubject();

      if (subject == null || subject.isBlank()) {
        throw new JwtException("Missing subject");
      }

      return subject;
    } catch (JwtException exception) {
      throw new BusinessException(AuthErrorCode.GOOGLE_ID_TOKEN_INVALID);
    }
  }
}