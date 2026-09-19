package com.fanroute.sync.domain.chat.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import com.fanroute.sync.domain.chat.dto.ChatDto;
import com.fanroute.sync.domain.chat.service.ChatService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {
  @Mock ChatService service;
  @Mock CurrentUserResolver currentUser;
  @Mock SimpMessagingTemplate messaging;
  @Mock Jwt jwt;
  @Mock User user;

  @Test
  void markReadSendsReceiptToEveryActiveMember() {
    ChatDto.ReadReceiptResponse receipt = new ChatDto.ReadReceiptResponse(20L, 1L, 50L);
    when(currentUser.getCurrentUser(jwt)).thenReturn(user);
    when(service.markRead(user, 20L, 50L)).thenReturn(receipt);
    when(service.activeMemberIds(20L)).thenReturn(List.of(1L, 2L));

    controller().markRead(jwt, 20L, new ChatDto.MarkReadRequest(50L));

    verify(messaging).convertAndSendToUser("1", "/queue/chat.rooms/20/reads", receipt);
    verify(messaging).convertAndSendToUser("2", "/queue/chat.rooms/20/reads", receipt);
  }

  private ChatController controller() {
    return new ChatController(service, currentUser, messaging);
  }
}
