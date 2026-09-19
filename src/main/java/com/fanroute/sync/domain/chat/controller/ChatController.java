package com.fanroute.sync.domain.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.chat.dto.ChatDto;
import com.fanroute.sync.domain.chat.service.ChatService;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ChatController implements ChatApi {
  private final ChatService service;
  private final CurrentUserResolver currentUser;

  @Override
  public ResponseEntity<ApiResponse<List<ChatDto.RoomResponse>>> list(Jwt jwt) {
    return ApiResponse.ok(service.list(currentUser.getCurrentUser(jwt))).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<ChatDto.MessagePageResponse>> messages(Jwt jwt, Long roomId,
      Long beforeMessageId, int size) {
    return ApiResponse.ok(service.messages(currentUser.getCurrentUser(jwt), roomId, beforeMessageId, size))
        .toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> accept(Jwt jwt, Long roomId,
      ChatDto.AcceptMemberRequest request) {
    service.accept(currentUser.getCurrentUser(jwt), roomId, request.commentId());
    return ApiResponse.<Void>ok(null).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> markRead(Jwt jwt, Long roomId,
      ChatDto.MarkReadRequest request) {
    service.markRead(currentUser.getCurrentUser(jwt), roomId, request.lastReadMessageId());
    return ApiResponse.<Void>ok(null).toResponseEntity();
  }
}
