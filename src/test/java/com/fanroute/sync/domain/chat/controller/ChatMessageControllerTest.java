package com.fanroute.sync.domain.chat.controller;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fanroute.sync.domain.chat.dto.ChatDto;
import com.fanroute.sync.domain.chat.service.ChatService;
import com.fanroute.sync.domain.notification.dto.NotificationDto;
import com.fanroute.sync.domain.notification.entity.NotificationType;
import com.fanroute.sync.domain.notification.service.NotificationService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class ChatMessageControllerTest {
  private static final Long ROOM_ID = 20L;

  @Mock ChatService chatService;
  @Mock UserService users;
  @Mock NotificationService notifications;
  @Mock SimpMessagingTemplate messaging;
  @Mock Principal principal;
  @Mock User sender;
  @Mock User recipientOne;
  @Mock User recipientTwo;

  @Test
  void sendDeliversEveryChatFrameBeforeIsolatedNotificationFailures() {
    ChatDto.MessageResponse message = message();
    NotificationDto.NotificationResponse notification = notification(2L);
    stubMessageSend(message, List.of(1L, 2L, 3L));
    when(users.getAccessibleUser(2L)).thenReturn(recipientOne);
    when(users.getAccessibleUser(3L)).thenReturn(recipientTwo);
    when(notifications.notifyChatMessage(recipientOne, "보낸이", "안녕하세요", ROOM_ID))
        .thenThrow(new IllegalStateException("notification failure"));
    when(notifications.notifyChatMessage(recipientTwo, "보낸이", "안녕하세요", ROOM_ID))
        .thenReturn(notification);

    controller().send(ROOM_ID, new ChatDto.SendMessageRequest("안녕하세요"), principal);

    InOrder order = inOrder(messaging, notifications);
    order.verify(messaging).convertAndSendToUser("1", "/queue/chat.rooms/20", message);
    order.verify(messaging).convertAndSendToUser("2", "/queue/chat.rooms/20", message);
    order.verify(messaging).convertAndSendToUser("3", "/queue/chat.rooms/20", message);
    order.verify(notifications).notifyChatMessage(recipientOne, "보낸이", "안녕하세요", ROOM_ID);
    order.verify(notifications).notifyChatMessage(recipientTwo, "보낸이", "안녕하세요", ROOM_ID);
    order.verify(messaging).convertAndSendToUser("3", "/queue/notifications", notification);
  }

  @Test
  void sendExcludesSenderFromNotificationsAndNotifiesOtherMembers() {
    ChatDto.MessageResponse message = message();
    NotificationDto.NotificationResponse firstNotification = notification(1L);
    NotificationDto.NotificationResponse secondNotification = notification(2L);
    stubMessageSend(message, List.of(1L, 2L, 3L));
    when(users.getAccessibleUser(2L)).thenReturn(recipientOne);
    when(users.getAccessibleUser(3L)).thenReturn(recipientTwo);
    when(notifications.notifyChatMessage(recipientOne, "보낸이", "안녕하세요", ROOM_ID))
        .thenReturn(firstNotification);
    when(notifications.notifyChatMessage(recipientTwo, "보낸이", "안녕하세요", ROOM_ID))
        .thenReturn(secondNotification);

    controller().send(ROOM_ID, new ChatDto.SendMessageRequest("안녕하세요"), principal);

    verify(users, times(1)).getAccessibleUser(1L);
    verify(notifications, never()).notifyChatMessage(sender, "보낸이", "안녕하세요", ROOM_ID);
    verify(messaging, never()).convertAndSendToUser("1", "/queue/notifications",
        firstNotification);
    verify(messaging).convertAndSendToUser("2", "/queue/notifications", firstNotification);
    verify(messaging).convertAndSendToUser("3", "/queue/notifications", secondNotification);
  }

  private void stubMessageSend(ChatDto.MessageResponse message, List<Long> memberIds) {
    when(principal.getName()).thenReturn("1");
    when(users.getAccessibleUser(1L)).thenReturn(sender);
    when(sender.getId()).thenReturn(1L);
    when(sender.getNickname()).thenReturn("보낸이");
    when(chatService.send(sender, ROOM_ID, "안녕하세요")).thenReturn(message);
    when(chatService.activeMemberIds(ROOM_ID)).thenReturn(memberIds);
  }

  private ChatMessageController controller() {
    return new ChatMessageController(chatService, users, notifications, messaging);
  }

  private ChatDto.MessageResponse message() {
    return new ChatDto.MessageResponse(10L, ROOM_ID, 1L, "보낸이", "안녕하세요",
        Instant.parse("2026-09-21T00:00:00Z"), 0L);
  }

  private NotificationDto.NotificationResponse notification(Long id) {
    return new NotificationDto.NotificationResponse(id, NotificationType.CHAT_MESSAGE, "새 채팅",
        "알림", ROOM_ID, Instant.parse("2026-09-21T00:00:00Z"), null);
  }
}
