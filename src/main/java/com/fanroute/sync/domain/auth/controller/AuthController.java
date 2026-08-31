package com.fanroute.sync.domain.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.dto.RefreshTokenDto;
import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.auth.service.GoogleLoginService;
import com.fanroute.sync.domain.auth.service.RefreshTokenService;
import com.fanroute.sync.domain.auth.service.TokenRefreshService;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

  private final GoogleLoginService googleLoginService;
  private final TokenRefreshService tokenRefreshService;
  private final RefreshTokenService refreshTokenService;
  private final RefreshTokenCookie refreshTokenCookie;

  @Override
  public ResponseEntity<ApiResponse<LoginDto.Response>> googleLogin(LoginDto.Request request) {
    return loginResponse(googleLoginService.login(request.authorizationCode()));
  }

  @Override
  public ResponseEntity<ApiResponse<RefreshTokenDto.Response>> refreshToken(String refreshToken) {
    if (refreshToken == null) {
      throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
    }
    TokenRefreshService.RefreshResult result = tokenRefreshService.refresh(refreshToken);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(
            result.refreshToken().value(), result.refreshToken().expiresIn()).toString())
        .body(ApiResponse.ok(result.response()));
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> logout(String refreshToken) {
    if (refreshToken != null) {
      refreshTokenService.revoke(refreshToken);
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
        .body(ApiResponse.ok());
  }

  @Override
  public ResponseEntity<ApiResponse<LoginDto.Response>> googleCallback(
      String authorizationCode, String state) {
    return loginResponse(googleLoginService.login(authorizationCode));
  }

  private ResponseEntity<ApiResponse<LoginDto.Response>> loginResponse(
      GoogleLoginService.LoginResult result) {
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(
            result.refreshToken().value(), result.refreshToken().expiresIn()).toString())
        .body(ApiResponse.ok(result.response()));
  }
}