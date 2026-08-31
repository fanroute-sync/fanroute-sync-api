package com.fanroute.sync.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.response.ErrorCode;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** {@link AuthDebugController}의 Swagger 문서 계약. local 프로파일에서만 존재하는 디버그 전용 API입니다. */
@RequestMapping("/api/v1/auth")
@Tag(name = "인증(로컬 디버그)", description = "프론트 연동 전 로컬 Swagger 테스트 전용 API — local 프로파일에서만 존재")
public interface AuthDebugApi {

  @Operation(
      summary = "Google 로그인 콜백 (로컬 전용)",
      description = "로컬 Swagger 테스트를 위한 Google OAuth 콜백입니다. local 프로파일에서만 등록됩니다.")
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
      @Parameter(description = "요청 위변조 검증용 상태 값 — 로컬 디버그 경로라 실제 검증하지 않음")
      @RequestParam(value = "state", required = false) String state);
}
