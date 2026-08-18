package com.fanroute.sync.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.dto.RefreshTokenDto;
import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.auth.service.GoogleLoginService;
import com.fanroute.sync.domain.auth.service.RefreshTokenService;
import com.fanroute.sync.domain.auth.service.TokenRefreshService;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.response.ErrorCode;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 클라이언트가 전달한 Google 인가 코드를 처리하는 공개 인증 API입니다.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "로컬 테스트: 로그인 스크립트 실행 → Google 로그인 → Access Token을 Swagger Authorize에 입력")
public class AuthController {

  private final GoogleLoginService googleLoginService;
  private final TokenRefreshService tokenRefreshService;
  private final RefreshTokenService refreshTokenService;

  /**
   * Google 로그인 성공 시 서비스 API에서 사용할 JWT Access Token을 반환합니다.
   */
  @Operation(summary = "운영용 Google 로그인", description = "클라이언트가 전달한 Google 인가 코드로 토큰을 발급합니다. 로컬 Swagger 테스트에서는 콜백이 코드를 이미 사용하므로 다시 호출하지 않습니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = ErrorCode.class,
      names = "INVALID_PARAMETER")
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
    return ApiResponse.ok(googleLoginService.login(request.authorizationCode())).toResponseEntity();
  }

  @Operation(summary = "2. 토큰 갱신", description = "로그인 콜백 응답의 Refresh Token으로 새로운 Access Token과 Refresh Token을 발급합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 갱신 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = ErrorCode.class,
      names = "INVALID_PARAMETER")
  @ApiErrorCodeExamples(
      type = AuthErrorCode.class,
      names = "REFRESH_TOKEN_INVALID")
  @ApiErrorCodeExamples(
      type = UserErrorCode.class,
      names = {"USER_SUSPENDED", "USER_WITHDRAWN"})
  @PostMapping("/token/refresh")
  public ResponseEntity<ApiResponse<RefreshTokenDto.Response>> refreshToken(
      @Valid @RequestBody RefreshTokenDto.Request request) {
    return ApiResponse.ok(tokenRefreshService.refresh(request.refreshToken())).toResponseEntity();
  }

  @Operation(summary = "로그아웃", description = "요청한 Refresh Token을 폐기합니다. 이미 폐기된 토큰도 성공 처리합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "로그아웃 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ErrorCode.class, names = "INVALID_PARAMETER")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "REFRESH_TOKEN_INVALID")
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @Valid @RequestBody RefreshTokenDto.Request request) {
    refreshTokenService.revoke(request.refreshToken());
    return ApiResponse.<Void>ok(null).toResponseEntity();
  }

  /**
   * TODO:: 프론트 완성 후 삭제
   * 프론트엔드가 없는 로컬 환경에서 Google 리디렉션을 직접 처리하기 위한 콜백입니다.
   */
  @Operation(summary = "1. Google 로그인 콜백 (자동 호출)", description = "로그인 스크립트가 연 Google 화면에서 로그인을 마치면 자동 호출됩니다. 응답의 Access Token을 Swagger Authorize에 입력하며, Try it out으로 직접 호출하지 않습니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = ErrorCode.class,
      names = "INVALID_PARAMETER")
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
      @Parameter(description = "Google에서 전달받은 인가 코드", required = true) @RequestParam("code") String authorizationCode,
      @Parameter(description = "요청 위변조 검증용 상태 값") @RequestParam(value = "state", required = false) String state) {
    LoginDto.Response response = googleLoginService.login(authorizationCode);

    return ApiResponse.ok(response).toResponseEntity();
  }
}
