package com.fanroute.sync.domain.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.notification.dto.NotificationDto;
import com.fanroute.sync.global.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RequestMapping("/api/v1/notifications")
@Tag(name = "알림", description = "인앱 알림과 FCM 디바이스 토큰")
@SecurityRequirement(name = "bearerAuth")
public interface NotificationApi {
  @GetMapping
  @Operation(summary = "알림 목록 조회")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
      description = "조회 성공", useReturnTypeSchema = true))
  ResponseEntity<ApiResponse<NotificationDto.ListResponse>> list(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @RequestParam(defaultValue = "30") int size);

  @PatchMapping("/{notificationId}/read")
  @Operation(summary = "알림 읽음 처리")
  ResponseEntity<ApiResponse<Void>> markRead(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @PathVariable Long notificationId);

  @PostMapping("/push-tokens")
  @Operation(summary = "FCM 디바이스 토큰 등록")
  ResponseEntity<ApiResponse<Void>> registerToken(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody NotificationDto.RegisterTokenRequest request);

  @DeleteMapping("/push-tokens")
  @Operation(summary = "FCM 디바이스 토큰 해제")
  ResponseEntity<ApiResponse<Void>> unregisterToken(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody NotificationDto.RegisterTokenRequest request);
}
