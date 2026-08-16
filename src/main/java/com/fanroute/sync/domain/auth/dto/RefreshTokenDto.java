package com.fanroute.sync.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RefreshTokenDto {

  public record Request(
      @NotBlank(message = "Refresh Token은 필수입니다.") String refreshToken) {

  }

  public record Response(
      String accessToken, String refreshToken, String tokenType, long expiresIn,
      long refreshTokenExpiresIn) {

  }
}
