package com.fanroute.sync.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.user.dto.UserProfileDto;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** {@link UserController}의 Swagger 문서 계약. */
@RequestMapping("/api/v1/users")
@Tag(name = "사용자", description = "현재 사용자의 프로필 조회 및 수정 API")
@SecurityRequirement(name = "bearerAuth")
public interface UserApi {

  @Operation(
      summary = "회원 탈퇴",
      description = "현재 사용자를 탈퇴 처리하고 Refresh Token을 폐기한 뒤 Cookie를 삭제합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "회원 탈퇴 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = UserErrorCode.class,
      names = {"USER_NOT_FOUND", "USER_SUSPENDED", "USER_WITHDRAWN"})
  @DeleteMapping("/me")
  ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal Jwt jwt);

  @Operation(summary = "내 프로필 조회")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "프로필 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = UserErrorCode.class,
      names = {"USER_NOT_FOUND", "USER_SUSPENDED", "USER_WITHDRAWN"})
  @GetMapping("/me")
  ResponseEntity<ApiResponse<UserProfileDto.Response>> getMyProfile(
      @AuthenticationPrincipal Jwt jwt);

  @Operation(summary = "닉네임 사용 가능 여부 조회")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "닉네임 사용 가능 여부 조회 성공",
          useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = UserErrorCode.class,
      names = {"USER_INVALID_NICKNAME", "USER_NOT_FOUND", "USER_SUSPENDED", "USER_WITHDRAWN"})
  @GetMapping("/nickname-availability")
  ResponseEntity<ApiResponse<UserProfileDto.NicknameAvailabilityResponse>> getNicknameAvailability(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam String nickname);

  @Operation(summary = "내 프로필 수정")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "프로필 수정 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = UserErrorCode.class,
      names = {
          "USER_INVALID_NICKNAME", "USER_DUPLICATE_NICKNAME", "USER_NOT_FOUND",
          "USER_SUSPENDED", "USER_WITHDRAWN"
      })
  @PatchMapping("/me")
  ResponseEntity<ApiResponse<UserProfileDto.Response>> updateMyProfile(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody UserProfileDto.UpdateRequest request);
}
