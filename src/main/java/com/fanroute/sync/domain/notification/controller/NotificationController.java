package com.fanroute.sync.domain.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.notification.dto.NotificationDto;
import com.fanroute.sync.domain.notification.service.NotificationService;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {
  private final NotificationService service;
  private final CurrentUserResolver currentUser;

  @Override
  public ResponseEntity<ApiResponse<NotificationDto.ListResponse>> list(Jwt jwt, int size) {
    return ApiResponse.ok(service.list(currentUser.getCurrentUser(jwt), size)).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> markRead(Jwt jwt, Long notificationId) {
    service.markRead(currentUser.getCurrentUser(jwt), notificationId);
    return ApiResponse.<Void>ok(null).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> registerToken(Jwt jwt,
      NotificationDto.RegisterTokenRequest request) {
    service.registerToken(currentUser.getCurrentUser(jwt), request.token());
    return ApiResponse.<Void>ok(null).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> unregisterToken(Jwt jwt,
      NotificationDto.RegisterTokenRequest request) {
    service.unregisterToken(currentUser.getCurrentUser(jwt), request.token());
    return ApiResponse.<Void>ok(null).toResponseEntity();
  }
}
