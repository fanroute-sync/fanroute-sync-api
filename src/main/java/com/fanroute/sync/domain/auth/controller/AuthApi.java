package com.fanroute.sync.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.auth.config.RefreshTokenCsrfFilter;
import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.dto.RefreshTokenDto;
import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.response.ErrorCode;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** {@link AuthController}의 Swagger 문서 계약. 컨트롤러 가독성을 위해 애노테이션을 이 인터페이스로 분리합니다. */
@RequestMapping("/api/v1/auth")
@Tag(name = "인증", description = "Google 로그인과 서비스 토큰 갱신 API")
public interface AuthApi {

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
  ResponseEntity<ApiResponse<LoginDto.Response>> googleLogin(
      @Valid @RequestBody LoginDto.Request request);

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
          schema = @Schema(defaultValue = "XMLHttpRequest"),
          required = true)
  })
  @PostMapping("/token/refresh")
  ResponseEntity<ApiResponse<RefreshTokenDto.Response>> refreshToken(
      @Parameter(hidden = true)
      @CookieValue(value = RefreshTokenCookie.NAME, required = false) String refreshToken);

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
          schema = @Schema(defaultValue = "XMLHttpRequest"),
          required = true)
  })
  @PostMapping("/logout")
  ResponseEntity<ApiResponse<Void>> logout(
      @Parameter(hidden = true)
      @CookieValue(value = RefreshTokenCookie.NAME, required = false) String refreshToken);

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
  ResponseEntity<ApiResponse<LoginDto.Response>> googleCallback(
      @Parameter(description = "Google에서 전달받은 인가 코드", required = true)
      @RequestParam("code") String authorizationCode,
      @Parameter(description = "요청 위변조 검증용 상태 값")
      @RequestParam(value = "state", required = false) String state);
}