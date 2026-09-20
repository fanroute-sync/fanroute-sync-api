package com.fanroute.sync.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.chat.entity.ChatRoom;
import com.fanroute.sync.domain.chat.entity.ChatRoomMember;
import com.fanroute.sync.domain.chat.entity.ChatMemberRole;
import com.fanroute.sync.domain.chat.entity.ChatMessage;
import com.fanroute.sync.domain.chat.dto.ChatDto;
import com.fanroute.sync.domain.chat.exception.ChatErrorCode;
import com.fanroute.sync.domain.chat.repository.ChatMessageRepository;
import com.fanroute.sync.domain.chat.repository.ChatRoomMemberRepository;
import com.fanroute.sync.domain.chat.repository.ChatRoomRepository;
import com.fanroute.sync.domain.community.entity.Comment;
import com.fanroute.sync.domain.community.entity.Post;
import com.fanroute.sync.domain.community.entity.PostType;
import com.fanroute.sync.domain.community.repository.CommentRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
  @Mock ChatRoomRepository rooms;
  @Mock ChatRoomMemberRepository members;
  @Mock ChatMessageRepository messages;
  @Mock CommentRepository comments;

  @Test
  void acceptRejectsWhenActiveMembersAlreadyReachCapacity() {
    User owner = user(1L);
    User applicant = user(2L);
    Post post = Post.create(owner, PostType.COMPANION, "동행", null, List.of(), null, null,
        java.time.LocalDate.now(), 2, null);
    ReflectionTestUtils.setField(post, "id", 10L);
    ChatRoom room = ChatRoom.createCompanion(post, owner);
    ReflectionTestUtils.setField(room, "id", 20L);
    Comment comment = Comment.create(post, applicant, null, "신청합니다");
    ReflectionTestUtils.setField(comment, "id", 30L);
    when(rooms.findByIdForUpdate(20L)).thenReturn(Optional.of(room));
    when(comments.findById(30L)).thenReturn(Optional.of(comment));
    when(members.findByChatRoomIdAndUserId(20L, 2L)).thenReturn(Optional.empty());
    when(members.countByChatRoomIdAndLeftAtIsNull(20L)).thenReturn(2L);

    assertThatThrownBy(() -> service().accept(owner, 20L, 30L))
        .isInstanceOf(BusinessException.class)
        .extracting(error -> ((BusinessException) error).getErrorCode())
        .isEqualTo(ChatErrorCode.CAPACITY_REACHED);

    verify(members, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void markReadReturnsTheUpdatedReadCursor() {
    User user = user(1L);
    Post post = Post.create(user, PostType.COMPANION, "동행", null, List.of(), null, null,
        java.time.LocalDate.now(), 2, null);
    ChatRoom room = ChatRoom.createCompanion(post, user);
    ReflectionTestUtils.setField(room, "id", 20L);
    ChatRoomMember member = ChatRoomMember.create(room, user, ChatMemberRole.OWNER);
    when(members.findByChatRoomIdAndUserId(20L, 1L)).thenReturn(Optional.of(member));
    when(messages.existsByIdAndChatRoomId(50L, 20L)).thenReturn(true);

    ChatDto.ReadReceiptResponse response = service().markRead(user, 20L, 50L);

    assertThat(response.roomId()).isEqualTo(20L);
    assertThat(response.userId()).isEqualTo(1L);
    assertThat(response.lastReadMessageId()).isEqualTo(50L);
  }

  @Test
  void listSkipsMessageLookupWhenThereAreNoActiveRooms() {
    User user = user(1L);
    when(members.findByUserIdAndLeftAtIsNullOrderByChatRoomLastMessageAtDesc(1L))
        .thenReturn(List.of());

    assertThat(service().list(user)).isEmpty();

    verifyNoInteractions(messages);
  }

  @Test
  void listMatchesLastMessageContentByRoomAndPreservesUnreadAndTime() {
    User user = user(1L);
    ChatRoom firstRoom = room(user, 20L);
    ChatRoom secondRoom = room(user, 21L);
    ChatRoom emptyRoom = room(user, 22L);
    Instant firstSentAt = Instant.parse("2026-09-21T01:02:03Z");
    Instant secondSentAt = Instant.parse("2026-09-21T02:03:04Z");
    firstRoom.updateLastMessage(101L, firstSentAt);
    secondRoom.updateLastMessage(202L, secondSentAt);
    ChatMessage firstMessage = message(firstRoom, user, 101L, "첫 번째 메시지");
    ChatMessage secondMessage = message(secondRoom, user, 202L, "두 번째 메시지");

    when(members.findByUserIdAndLeftAtIsNullOrderByChatRoomLastMessageAtDesc(1L))
        .thenReturn(List.of(member(firstRoom, user), member(secondRoom, user), member(emptyRoom, user)));
    when(messages.countByChatRoomIdAndIdGreaterThan(20L, 0L)).thenReturn(3L);
    when(messages.countByChatRoomIdAndIdGreaterThan(21L, 0L)).thenReturn(1L);
    when(messages.countByChatRoomIdAndIdGreaterThan(22L, 0L)).thenReturn(0L);
    when(messages.findAllById(List.of(101L, 202L))).thenReturn(List.of(secondMessage, firstMessage));

    List<ChatDto.RoomResponse> result = service().list(user);

    assertThat(result).extracting(ChatDto.RoomResponse::roomId)
        .containsExactly(20L, 21L, 22L);
    assertThat(result.get(0).lastMessage()).isEqualTo("첫 번째 메시지");
    assertThat(result.get(1).lastMessage()).isEqualTo("두 번째 메시지");
    assertThat(result.get(2).lastMessage()).isNull();
    assertThat(result).extracting(ChatDto.RoomResponse::lastMessageAt)
        .containsExactly(firstSentAt, secondSentAt, null);
    assertThat(result).extracting(ChatDto.RoomResponse::unreadCount)
        .containsExactly(3L, 1L, 0L);

    verify(messages).findAllById(List.of(101L, 202L));
  }

  @Test
  void messagesReturnsAscendingFirstPageAndCursorForNextPage() {
    User user = user(1L);
    ChatRoom room = room(user, 20L);
    when(members.findByChatRoomIdAndUserId(20L, 1L)).thenReturn(Optional.of(member(room, user)));
    when(messages.findByChatRoomIdOrderByIdDesc(20L, PageRequest.of(0, 3)))
        .thenReturn(List.of(message(room, user, 3L), message(room, user, 2L), message(room, user, 1L)));

    ChatDto.MessagePageResponse response = service().messages(user, 20L, null, 2);

    assertThat(response.messages()).extracting(ChatDto.MessageResponse::id).containsExactly(2L, 3L);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextBeforeMessageId()).isEqualTo(2L);
  }

  @Test
  void messagesReturnsCursorPageInAscendingOrderWithoutNextCursor() {
    User user = user(1L);
    ChatRoom room = room(user, 20L);
    when(members.findByChatRoomIdAndUserId(20L, 1L)).thenReturn(Optional.of(member(room, user)));
    when(messages.findByChatRoomIdAndIdLessThanOrderByIdDesc(20L, 2L, PageRequest.of(0, 3)))
        .thenReturn(List.of(message(room, user, 1L)));

    ChatDto.MessagePageResponse response = service().messages(user, 20L, 2L, 2);

    assertThat(response.messages()).extracting(ChatDto.MessageResponse::id).containsExactly(1L);
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextBeforeMessageId()).isNull();
  }

  @Test
  void messagesHandlesEmptyAndSingleMessagePages() {
    User user = user(1L);
    ChatRoom room = room(user, 20L);
    when(members.findByChatRoomIdAndUserId(20L, 1L)).thenReturn(Optional.of(member(room, user)));
    when(messages.findByChatRoomIdOrderByIdDesc(20L, PageRequest.of(0, 3))).thenReturn(List.of());

    ChatDto.MessagePageResponse empty = service().messages(user, 20L, null, 2);

    assertThat(empty.messages()).isEmpty();
    assertThat(empty.hasNext()).isFalse();
    assertThat(empty.nextBeforeMessageId()).isNull();

    when(messages.findByChatRoomIdOrderByIdDesc(20L, PageRequest.of(0, 2)))
        .thenReturn(List.of(message(room, user, 1L)));

    ChatDto.MessagePageResponse single = service().messages(user, 20L, null, 1);

    assertThat(single.messages()).extracting(ChatDto.MessageResponse::id).containsExactly(1L);
    assertThat(single.hasNext()).isFalse();
    assertThat(single.nextBeforeMessageId()).isNull();
  }

  private User user(Long id) {
    User user = UserFixture.activeUser();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private ChatRoom room(User owner, Long id) {
    Post post = Post.create(owner, PostType.COMPANION, "동행", null, List.of(), null, null,
        java.time.LocalDate.now(), 2, null);
    ChatRoom room = ChatRoom.createCompanion(post, owner);
    ReflectionTestUtils.setField(room, "id", id);
    return room;
  }

  private ChatRoomMember member(ChatRoom room, User user) {
    return ChatRoomMember.create(room, user, ChatMemberRole.OWNER);
  }

  private ChatMessage message(ChatRoom room, User sender, Long id) {
    return message(room, sender, id, "메시지 " + id);
  }

  private ChatMessage message(ChatRoom room, User sender, Long id, String content) {
    ChatMessage message = ChatMessage.createText(room, sender, content);
    ReflectionTestUtils.setField(message, "id", id);
    return message;
  }

  private ChatService service() {
    return new ChatService(rooms, members, messages, comments);
  }
}
