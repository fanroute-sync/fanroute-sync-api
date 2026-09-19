package com.fanroute.sync.domain.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.user.dto.UserProfileDto;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.domain.user.service.SessionCookieClearer;
import com.fanroute.sync.domain.user.service.UserService;
import com.fanroute.sync.domain.user.service.PushDeviceService;
import com.fanroute.sync.domain.user.dto.PushDeviceDto;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

  private final CurrentUserResolver currentUserResolver;
  private final UserService userService;
  private final SessionCookieClearer sessionCookieClearer;
  private final PushDeviceService pushDeviceService;

  @Override
  public ResponseEntity<ApiResponse<Void>> withdraw(Jwt jwt) {
    User currentUser = currentUserResolver.getCurrentUser(jwt);
    userService.withdrawUser(currentUser.getId());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, sessionCookieClearer.clear())
        .body(ApiResponse.ok());
  }

  @Override
  public ResponseEntity<ApiResponse<UserProfileDto.Response>> getMyProfile(Jwt jwt) {
    User user = currentUserResolver.getCurrentUser(jwt);
    return ApiResponse.ok(UserProfileDto.Response.from(user)).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<UserProfileDto.NicknameAvailabilityResponse>>
  getNicknameAvailability(Jwt jwt, String nickname) {
    User user = currentUserResolver.getCurrentUser(jwt);
    String normalized = User.normalizeNickname(nickname);
    boolean available = userService.isNicknameAvailable(user, normalized);
    return ApiResponse.ok(
        new UserProfileDto.NicknameAvailabilityResponse(normalized, available)).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<UserProfileDto.Response>> updateMyProfile(
      Jwt jwt, UserProfileDto.UpdateRequest request) {
    User currentUser = currentUserResolver.getCurrentUser(jwt);
    User updatedUser = userService.updateNickname(currentUser.getId(), request.nickname());
    return ApiResponse.ok(UserProfileDto.Response.from(updatedUser)).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> registerPushDevice(Jwt jwt,
      PushDeviceDto.RegisterRequest request) {
    pushDeviceService.register(currentUserResolver.getCurrentUser(jwt), request.registrationToken());
    return ApiResponse.<Void>ok().toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> unregisterPushDevice(Jwt jwt,
      PushDeviceDto.RegisterRequest request) {
    pushDeviceService.unregister(currentUserResolver.getCurrentUser(jwt), request.registrationToken());
    return ApiResponse.<Void>ok().toResponseEntity();
  }
}
