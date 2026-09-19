package com.fanroute.sync.domain.chat.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ChatDto {
  private ChatDto() {}

  public record AcceptMemberRequest(@NotNull Long commentId) {}
  public record MarkReadRequest(@NotNull Long lastReadMessageId) {}
  public record SendMessageRequest(@NotBlank @Size(max = 1000) String content) {}
  public record RoomResponse(Long roomId, Long companionPostId, String title, Instant lastMessageAt,
      String lastMessage, long unreadCount) {}
  public record MessageResponse(Long id, Long roomId, Long senderId, String senderNickname,
      String content, Instant createdAt) {}
  public record MessagePageResponse(List<MessageResponse> messages, boolean hasNext,
      Long nextBeforeMessageId) {}
}
