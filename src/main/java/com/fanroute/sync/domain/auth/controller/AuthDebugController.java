package com.fanroute.sync.domain.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.service.GoogleLoginService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * 프론트엔드 로그인 연동 전, 로컬 Swagger에서 Google OAuth 콜백을 직접 확인하기 위한 디버그 전용 엔드포인트입니다.
 * <p>
 * {@code local} 프로파일에서만 빈으로 등록되어 운영 환경에는 이 경로 자체가 존재하지 않습니다(요청 시 404).
 * </p>
 */
@RestController
@RequiredArgsConstructor
@Profile("local")
public class AuthDebugController implements AuthDebugApi {

  private final GoogleLoginService googleLoginService;
  private final RefreshTokenCookie refreshTokenCookie;

  @Override
  public ResponseEntity<ApiResponse<LoginDto.Response>> googleCallback(
      String authorizationCode, String state) {
    GoogleLoginService.LoginResult result = googleLoginService.login(authorizationCode);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(
            result.refreshToken().value(), result.refreshToken().expiresIn()).toString())
        .body(ApiResponse.ok(result.response()));
  }
}
