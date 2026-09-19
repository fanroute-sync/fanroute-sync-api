package com.fanroute.sync.domain.notification.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.notification.dto.NotificationDto;
import com.fanroute.sync.domain.notification.entity.Notification;
import com.fanroute.sync.domain.notification.entity.PushToken;
import com.fanroute.sync.domain.notification.repository.NotificationRepository;
import com.fanroute.sync.domain.notification.repository.PushTokenRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
  private final NotificationRepository notifications;
  private final PushTokenRepository tokens;
  private final FirebasePushService firebasePush;

  public NotificationDto.ListResponse list(User user, int size) {
    if (size < 1 || size > 100) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    List<NotificationDto.NotificationResponse> response = notifications
        .findByUserIdOrderByIdDesc(user.getId(), PageRequest.of(0, size)).stream().map(this::toResponse).toList();
    return new NotificationDto.ListResponse(response, notifications.countByUserIdAndReadAtIsNull(user.getId()));
  }

  @Transactional
  public void markRead(User user, Long notificationId) {
    Notification notification = notifications.findByIdAndUserId(notificationId, user.getId())
        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
    notification.markRead(Instant.now());
  }

  @Transactional
  public void registerToken(User user, String token) {
    String normalized = token.trim();
    tokens.findByToken(normalized).ifPresentOrElse(existing -> existing.changeOwner(user),
        () -> tokens.save(PushToken.create(user, normalized)));
  }

  @Transactional
  public void unregisterToken(User user, String token) {
    tokens.deleteByUserIdAndToken(user.getId(), token.trim());
  }

  @Transactional
  public void notifyChatMessage(User recipient, String senderName, String content, Long roomId) {
    Notification notification = notifications.save(Notification.chatMessage(recipient, senderName, content, roomId));
    firebasePush.sendChatMessage(recipient.getId(), notification.getTitle(), notification.getContent(), roomId);
  }

  private NotificationDto.NotificationResponse toResponse(Notification notification) {
    return new NotificationDto.NotificationResponse(notification.getId(), notification.getType(),
        notification.getTitle(), notification.getContent(), notification.getResourceId(),
        notification.getCreatedAt(), notification.getReadAt());
  }
}
