package com.fanroute.sync.domain.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.auth.config.RefreshTokenCsrfFilter;
import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.dto.RefreshTokenDto;
import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.auth.service.GoogleLoginService;
import com.fanroute.sync.domain.auth.service.RefreshTokenService;
import com.fanroute.sync.domain.auth.service.TokenRefreshService;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.response.ErrorCode;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "Google 로그인과 서비스 토큰 갱신 API")
public class AuthController {

  private final GoogleLoginService googleLoginService;
  private final TokenRefreshService tokenRefreshService;
  private final RefreshTokenService refreshTokenService;
  private final RefreshTokenCookie refreshTokenCookie;

  @Operation(
      summary = "Google 로그인",
      description = "Access Token은 응답 본문으로, Refresh Token은 HttpOnly Cookie로 발급합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "로그인 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ErrorCode.class, names = "INVALID_PARAMETER")
  @ApiErrorCodeExamples(
      type = AuthErrorCode.class,
      names = {
          "GOOGLE_AUTHORIZATION_CODE_INVALID",
          "GOOGLE_ID_TOKEN_INVALID",
          "GOOGLE_API_UNAVAILABLE"
      })
  @ApiErrorCodeExamples(
      type = UserErrorCode.class,
      names = {"USER_SUSPENDED", "USER_WITHDRAWN", "USER_REGISTRATION_CONFLICT"})
  @PostMapping("/google")
  public ResponseEntity<ApiResponse<LoginDto.Response>> googleLogin(
      @Valid @RequestBody LoginDto.Request request) {
    return loginResponse(googleLoginService.login(request.authorizationCode()));
  }

  @Operation(
      summary = "토큰 갱신",
      description = "Refresh Token Cookie를 회전하고 새 Access Token을 응답합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "토큰 갱신 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = AuthErrorCode.class,
      names = {"REFRESH_TOKEN_INVALID", "CSRF_HEADER_REQUIRED"})
  @ApiErrorCodeExamples(
      type = UserErrorCode.class,
      names = {"USER_SUSPENDED", "USER_WITHDRAWN"})
  @Parameters({
      @Parameter(
          name = RefreshTokenCsrfFilter.HEADER_NAME,
          description = "Cookie 기반 인증 요청을 구분하는 CSRF 방어 헤더",
          in = ParameterIn.HEADER,
          required = true)
  })
  @PostMapping("/token/refresh")
  public ResponseEntity<ApiResponse<RefreshTokenDto.Response>> refreshToken(
      @Parameter(
          name = RefreshTokenCookie.NAME,
          description = "HttpOnly Cookie로 전달되는 Refresh Token",
          in = ParameterIn.COOKIE,
          required = true)
      @CookieValue(value = RefreshTokenCookie.NAME, required = false) String refreshToken) {
    if (refreshToken == null) {
      throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
    }
    TokenRefreshService.RefreshResult result = tokenRefreshService.refresh(refreshToken);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(
            result.refreshToken().value(), result.refreshToken().expiresIn()).toString())
        .body(ApiResponse.ok(result.response()));
  }

  @Operation(
      summary = "로그아웃",
      description = "Refresh Token을 폐기하고 Cookie를 삭제합니다. Cookie가 없어도 성공합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "로그아웃 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = AuthErrorCode.class,
      names = {"REFRESH_TOKEN_INVALID", "CSRF_HEADER_REQUIRED"})
  @Parameters({
      @Parameter(
          name = RefreshTokenCsrfFilter.HEADER_NAME,
          description = "Cookie 기반 인증 요청을 구분하는 CSRF 방어 헤더",
          in = ParameterIn.HEADER,
          required = true)
  })
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @Parameter(
          name = RefreshTokenCookie.NAME,
          description = "폐기할 Refresh Token Cookie",
          in = ParameterIn.COOKIE)
      @CookieValue(value = RefreshTokenCookie.NAME, required = false) String refreshToken) {
    if (refreshToken != null) {
      refreshTokenService.revoke(refreshToken);
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
        .body(ApiResponse.ok());
  }

  @Operation(
      summary = "Google 로그인 콜백",
      description = "로컬 Swagger 테스트를 위한 Google OAuth 콜백입니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "로그인 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ErrorCode.class, names = "INVALID_PARAMETER")
  @ApiErrorCodeExamples(
      type = AuthErrorCode.class,
      names = {
          "GOOGLE_AUTHORIZATION_CODE_INVALID",
          "GOOGLE_ID_TOKEN_INVALID",
          "GOOGLE_API_UNAVAILABLE"
      })
  @ApiErrorCodeExamples(
      type = UserErrorCode.class,
      names = {"USER_SUSPENDED", "USER_WITHDRAWN", "USER_REGISTRATION_CONFLICT"})
  @GetMapping("/google/callback")
  public ResponseEntity<ApiResponse<LoginDto.Response>> googleCallback(
      @Parameter(description = "Google에서 전달받은 인가 코드", required = true)
      @RequestParam("code") String authorizationCode,
      @Parameter(description = "요청 위변조 검증용 상태 값")
      @RequestParam(value = "state", required = false) String state) {
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
