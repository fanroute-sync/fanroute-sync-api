package com.fanroute.sync.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GoogleOAuthDto {

  /**
   * Google Token Endpoint 응답 중 로그인 검증에 필요한 값을 표현합니다.
   */
  public record TokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("id_token") String idToken,
      @JsonProperty("expires_in") long expiresIn,
      @JsonProperty("token_type") String tokenType) {

    public boolean hasValidIdToken() {
      return idToken == null || idToken.isBlank();
    }
  }

}