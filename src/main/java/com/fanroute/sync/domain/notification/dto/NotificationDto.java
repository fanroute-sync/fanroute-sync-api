package com.fanroute.sync.domain.notification.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fanroute.sync.domain.notification.entity.NotificationType;

public final class NotificationDto {
  private NotificationDto() {}
  public record RegisterTokenRequest(@NotBlank @Size(max = 512) String token) {}
  public record NotificationResponse(Long id, NotificationType type, String title, String content,
      Long resourceId, Instant createdAt, Instant readAt) {}
  public record ListResponse(List<NotificationResponse> notifications, long unreadCount) {}
}
