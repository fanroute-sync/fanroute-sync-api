package com.fanroute.sync.domain.chat.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.fanroute.sync.domain.chat.dto.ChatDto;
import com.fanroute.sync.domain.chat.service.ChatService;
import com.fanroute.sync.domain.notification.dto.NotificationDto;
import com.fanroute.sync.domain.notification.service.NotificationService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {
  private final ChatService chatService;
  private final UserService users;
  private final NotificationService notifications;
  private final SimpMessagingTemplate messaging;

  @MessageMapping("/chat.rooms/{roomId}/messages")
  public void send(@DestinationVariable Long roomId, @Valid ChatDto.SendMessageRequest request,
      Principal principal) {
    User user = users.getAccessibleUser(Long.valueOf(principal.getName()));
    ChatDto.MessageResponse message = chatService.send(user, roomId, request.content());
    List<Long> memberIds = chatService.activeMemberIds(roomId);

    for (Long memberId : memberIds) {
      messaging.convertAndSendToUser(memberId.toString(), "/queue/chat.rooms/" + roomId, message);
    }

    for (Long memberId : memberIds) {
      if (memberId.equals(user.getId())) {
        continue;
      }
      try {
        NotificationDto.NotificationResponse notification = notifications.notifyChatMessage(
            users.getAccessibleUser(memberId),
            user.getNickname(), message.content(), roomId);
        messaging.convertAndSendToUser(memberId.toString(), "/queue/notifications", notification);
      } catch (RuntimeException exception) {
        log.warn("Chat notification delivery failed: roomId={}, recipientId={}, exception={}",
            roomId, memberId, exception.getClass().getSimpleName());
      }
    }
  }
}
