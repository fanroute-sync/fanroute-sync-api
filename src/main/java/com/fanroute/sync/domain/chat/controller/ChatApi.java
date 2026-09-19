package com.fanroute.sync.domain.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.chat.dto.ChatDto;
import com.fanroute.sync.domain.chat.exception.ChatErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RequestMapping("/api/v1/chat/rooms")
@Tag(name = "채팅", description = "동행 모집 채팅방과 메시지")
@SecurityRequirement(name = "bearerAuth")
public interface ChatApi {
  @GetMapping
  @Operation(summary = "참여 중인 채팅방 목록 조회")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "조회 성공", useReturnTypeSchema = true))
  ResponseEntity<ApiResponse<List<ChatDto.RoomResponse>>> list(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt);

  @GetMapping("/{roomId}/messages")
  @Operation(summary = "채팅 메시지 이력 조회", description = "beforeMessageId 이전 메시지를 cursor 방식으로 조회")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "조회 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = ChatErrorCode.class, names = {"ROOM_NOT_FOUND", "FORBIDDEN"})
  ResponseEntity<ApiResponse<ChatDto.MessagePageResponse>> messages(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @PathVariable Long roomId, @RequestParam(required = false) Long beforeMessageId,
      @RequestParam(defaultValue = "50") int size);

  @PostMapping("/{roomId}/members")
  @Operation(summary = "댓글 작성자 채팅 참여 채택")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "채택 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = ChatErrorCode.class,
      names = {"ROOM_NOT_FOUND", "FORBIDDEN", "INVALID_COMMENT", "CAPACITY_REACHED"})
  ResponseEntity<ApiResponse<Void>> accept(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
      @Valid @RequestBody ChatDto.AcceptMemberRequest request);

  @PatchMapping("/{roomId}/read")
  @Operation(summary = "채팅 메시지 읽음 처리", description = "활성 참여자에게 읽음 커서를 실시간 전송")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "읽음 처리 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = ChatErrorCode.class, names = {"FORBIDDEN", "INVALID_MESSAGE"})
  ResponseEntity<ApiResponse<Void>> markRead(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
      @Valid @RequestBody ChatDto.MarkReadRequest request);
}
