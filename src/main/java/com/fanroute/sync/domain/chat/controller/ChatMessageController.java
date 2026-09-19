package com.fanroute.sync.domain.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.chat.dto.ChatDto;
import com.fanroute.sync.domain.chat.service.ChatService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {
  private final ChatService chatService;
  private final UserService users;
  private final SimpMessagingTemplate messaging;

  @MessageMapping("/chat.rooms/{roomId}/messages")
  public void send(@DestinationVariable Long roomId, @Valid ChatDto.SendMessageRequest request,
      Principal principal) {
    User user = users.getAccessibleUser(Long.valueOf(principal.getName()));
    ChatDto.MessageResponse message = chatService.send(user, roomId, request.content());
    for (Long memberId : chatService.activeMemberIds(roomId)) {
      messaging.convertAndSendToUser(memberId.toString(), "/queue/chat.rooms/" + roomId, message);
    }
  }
}
