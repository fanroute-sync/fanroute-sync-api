package com.fanroute.sync.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoginDto {

  /**
   * OAuth Authorization Code Flow 완료 후 클라이언트가 전달하는 로그인 요청입니다.
   */
  public record Request(
      @NotBlank(message = "인가 코드는 필수입니다.") String authorizationCode) {

  }

  /**
   * 소셜 로그인 성공 후 클라이언트에 반환하는 서비스 인증 결과입니다.
   */
  public record Response(
      String accessToken, String tokenType, long expiresIn, Long userId, boolean newUser) {

    public static Response of(String token, Long userId, boolean newUser, long ttlSeconds) {
      return new Response(
          token,
          "Bearer",
          ttlSeconds,
          userId,
          newUser);
    }
  }

}
