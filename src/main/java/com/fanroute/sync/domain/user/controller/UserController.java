package com.fanroute.sync.domain.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.auth.controller.RefreshTokenCookie;
import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.auth.service.CurrentUserService;
import com.fanroute.sync.domain.user.dto.UserProfileDto;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.domain.user.service.UserService;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "현재 사용자의 프로필 조회 및 수정 API")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final CurrentUserService currentUserService;
  private final UserService userService;
  private final RefreshTokenCookie refreshTokenCookie;

  @Operation(
      summary = "회원 탈퇴",
      description = "현재 사용자를 탈퇴 처리하고 Refresh Token을 폐기한 뒤 Cookie를 삭제합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "회원 탈퇴 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_TOKEN_INVALID")
  @ApiErrorCodeExamples(
      type = UserErrorCode.class,
      names = {"USER_NOT_FOUND", "USER_SUSPENDED", "USER_WITHDRAWN"})
  @DeleteMapping("/me")
  public ResponseEntity<ApiResponse<Void>> withdraw(
      @AuthenticationPrincipal Jwt jwt) {
    User currentUser = currentUserService.getCurrentUser(jwt);
    userService.withdrawUser(currentUser.getId());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
        .body(ApiResponse.ok());
  }

  @Operation(summary = "내 프로필 조회")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "프로필 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_TOKEN_INVALID")
  @ApiErrorCodeExamples(
      type = UserErrorCode.class,
      names = {"USER_NOT_FOUND", "USER_SUSPENDED", "USER_WITHDRAWN"})
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserProfileDto.Response>> getMyProfile(
      @AuthenticationPrincipal Jwt jwt) {
    User user = currentUserService.getCurrentUser(jwt);
    return ApiResponse.ok(UserProfileDto.Response.from(user)).toResponseEntity();
  }

  @Operation(summary = "닉네임 사용 가능 여부 조회")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "닉네임 사용 가능 여부 조회 성공",
          useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_TOKEN_INVALID")
  @ApiErrorCodeExamples(
      type = UserErrorCode.class,
      names = {"USER_INVALID_NICKNAME", "USER_NOT_FOUND", "USER_SUSPENDED", "USER_WITHDRAWN"})
  @GetMapping("/nickname-availability")
  public ResponseEntity<ApiResponse<UserProfileDto.NicknameAvailabilityResponse>>
  getNicknameAvailability(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam String nickname) {
    User user = currentUserService.getCurrentUser(jwt);
    String normalized = User.normalizeNickname(nickname);
    boolean available = userService.isNicknameAvailable(user, normalized);
    return ApiResponse.ok(
        new UserProfileDto.NicknameAvailabilityResponse(normalized, available)).toResponseEntity();
  }

  @Operation(summary = "내 프로필 수정")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "프로필 수정 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_TOKEN_INVALID")
  @ApiErrorCodeExamples(
      type = UserErrorCode.class,
      names = {
          "USER_INVALID_NICKNAME", "USER_DUPLICATE_NICKNAME", "USER_NOT_FOUND",
          "USER_SUSPENDED", "USER_WITHDRAWN"
      })
  @PatchMapping("/me")
  public ResponseEntity<ApiResponse<UserProfileDto.Response>> updateMyProfile(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody UserProfileDto.UpdateRequest request) {
    User currentUser = currentUserService.getCurrentUser(jwt);
    User updatedUser = userService.updateNickname(currentUser.getId(), request.nickname());
    return ApiResponse.ok(UserProfileDto.Response.from(updatedUser)).toResponseEntity();
  }
}
