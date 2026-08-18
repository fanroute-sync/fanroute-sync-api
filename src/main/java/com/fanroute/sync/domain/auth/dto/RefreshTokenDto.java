package com.fanroute.sync.domain.auth.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RefreshTokenDto {

  public record Response(String accessToken, String tokenType, long expiresIn) {

  }
}
