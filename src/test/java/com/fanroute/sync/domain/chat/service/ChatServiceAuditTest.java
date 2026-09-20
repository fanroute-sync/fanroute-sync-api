package com.fanroute.sync.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import com.fanroute.sync.domain.chat.dto.ChatDto;
import com.fanroute.sync.domain.chat.entity.ChatMemberRole;
import com.fanroute.sync.domain.chat.entity.ChatMessage;
import com.fanroute.sync.domain.chat.entity.ChatRoom;
import com.fanroute.sync.domain.chat.entity.ChatRoomMember;
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
class ChatServiceAuditTest {
  @Mock ChatRoomRepository rooms;
  @Mock ChatRoomMemberRepository members;
  @Mock ChatMessageRepository messages;
  @Mock CommentRepository comments;

  @Test
  void createCompanionRoomRegistersOwner() {
    User owner = user(1L);
    Post post = post(owner, 10L, 3);
    ChatRoom savedRoom = ChatRoom.createCompanion(post, owner);
    when(rooms.save(any(ChatRoom.class))).thenReturn(savedRoom);

    service().createCompanionRoom(post);

    ArgumentCaptor<ChatRoomMember> captor = ArgumentCaptor.forClass(ChatRoomMember.class);
    verify(members).save(captor.capture());
    assertThat(captor.getValue().getChatRoom()).isSameAs(savedRoom);
    assertThat(captor.getValue().getUser()).isSameAs(owner);
    assertThat(captor.getValue().getRole()).isEqualTo(ChatMemberRole.OWNER);
  }

  @Test
  void acceptAddsValidTopLevelCommenterAndRepeatedAcceptIsIdempotent() {
    User owner = user(1L);
    User applicant = user(2L);
    Post post = post(owner, 10L, 2);
    ChatRoom room = room(post, owner, 20L);
    Comment comment = comment(post, applicant, null, 30L);
    when(rooms.findByIdForUpdate(20L)).thenReturn(Optional.of(room));
    when(comments.findById(30L)).thenReturn(Optional.of(comment));
    when(members.findByChatRoomIdAndUserId(20L, 2L)).thenReturn(Optional.empty());
    when(members.countByChatRoomIdAndLeftAtIsNull(20L)).thenReturn(1L);

    service().accept(owner, 20L, 30L);

    ArgumentCaptor<ChatRoomMember> captor = ArgumentCaptor.forClass(ChatRoomMember.class);
    verify(members).save(captor.capture());
    assertThat(captor.getValue().getUser()).isSameAs(applicant);
    assertThat(captor.getValue().getRole()).isEqualTo(ChatMemberRole.MEMBER);

    ChatRoomMember existing = ChatRoomMember.create(room, applicant, ChatMemberRole.MEMBER);
    when(members.findByChatRoomIdAndUserId(20L, 2L)).thenReturn(Optional.of(existing));
    service().accept(owner, 20L, 30L);
    verify(members, times(1)).save(captor.getValue());
  }

  @Test
  void acceptRejectsOwnerCommentAndNonTopLevelOrOtherPostComment() {
    User owner = user(1L);
    User applicant = user(2L);
    Post post = post(owner, 10L, 2);
    Post otherPost = post(owner, 11L, 2);
    ChatRoom room = room(post, owner, 20L);
    Comment ownerComment = comment(post, owner, null, 31L);
    Comment parent = comment(post, applicant, null, 32L);
    Comment reply = comment(post, applicant, parent, 33L);
    Comment otherPostComment = comment(otherPost, applicant, null, 34L);
    when(rooms.findByIdForUpdate(20L)).thenReturn(Optional.of(room));

    for (Comment comment : List.of(ownerComment, reply, otherPostComment)) {
      when(comments.findById(comment.getId())).thenReturn(Optional.of(comment));
      assertThatThrownBy(() -> service().accept(owner, 20L, comment.getId()))
          .isInstanceOfSatisfying(BusinessException.class,
              error -> assertThat(error.getErrorCode()).isEqualTo(ChatErrorCode.INVALID_COMMENT));
    }
    verify(members, never()).save(any(ChatRoomMember.class));
  }

  @Test
  void acceptRejectsWhenCapacityIsFull() {
    User owner = user(1L);
    User applicant = user(2L);
    Post post = post(owner, 10L, 1);
    ChatRoom room = room(post, owner, 20L);
    Comment comment = comment(post, applicant, null, 30L);
    when(rooms.findByIdForUpdate(20L)).thenReturn(Optional.of(room));
    when(comments.findById(30L)).thenReturn(Optional.of(comment));
    when(members.findByChatRoomIdAndUserId(20L, 2L)).thenReturn(Optional.empty());
    when(members.countByChatRoomIdAndLeftAtIsNull(20L)).thenReturn(1L);

    assertThatThrownBy(() -> service().accept(owner, 20L, 30L))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.getErrorCode()).isEqualTo(ChatErrorCode.CAPACITY_REACHED));
    verify(members, never()).save(any(ChatRoomMember.class));
  }

  @Test
  void acceptRejectsNonOwner() {
    User owner = user(1L);
    User applicant = user(2L);
    ChatRoom room = room(post(owner, 10L, 2), owner, 20L);
    when(rooms.findByIdForUpdate(20L)).thenReturn(Optional.of(room));

    assertThatThrownBy(() -> service().accept(applicant, 20L, 30L))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.getErrorCode()).isEqualTo(ChatErrorCode.FORBIDDEN));
    verify(members, never()).save(any(ChatRoomMember.class));
  }

  @Test
  void sendRejectsEmptyAndTooLongContentAndTrimsValidContent() {
    User owner = user(1L);
    Post post = post(owner, 10L, 2);
    ChatRoom room = room(post, owner, 20L);
    ChatRoomMember member = ChatRoomMember.create(room, owner, ChatMemberRole.OWNER);
    when(rooms.findById(20L)).thenReturn(Optional.of(room));
    when(members.findByChatRoomIdAndUserId(20L, 1L)).thenReturn(Optional.of(member));
    when(messages.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

    assertInvalidMessage(() -> service().send(owner, 20L, "   "));
    assertInvalidMessage(() -> service().send(owner, 20L, "x".repeat(1001)));

    ChatDto.MessageResponse response = service().send(owner, 20L, "  hello  ");
    assertThat(response.content()).isEqualTo("hello");
    ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
    verify(messages).save(captor.capture());
    assertThat(captor.getValue().getContent()).isEqualTo("hello");
  }

  @Test
  void sendRejectsNonMember() {
    User owner = user(1L);
    Post post = post(owner, 10L, 2);
    ChatRoom room = room(post, owner, 20L);
    User outsider = user(2L);
    when(rooms.findById(20L)).thenReturn(Optional.of(room));
    when(members.findByChatRoomIdAndUserId(20L, 2L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().send(outsider, 20L, "hello"))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.getErrorCode()).isEqualTo(ChatErrorCode.FORBIDDEN));
  }

  @Test
  void markReadRejectsMessageFromAnotherRoomAndKeepsCursorMonotonic() {
    User owner = user(1L);
    ChatRoom room = room(post(owner, 10L, 2), owner, 20L);
    ChatRoomMember member = ChatRoomMember.create(room, owner, ChatMemberRole.OWNER);
    when(members.findByChatRoomIdAndUserId(20L, 1L)).thenReturn(Optional.of(member));
    when(messages.existsByIdAndChatRoomId(99L, 20L)).thenReturn(false);
    assertThatThrownBy(() -> service().markRead(owner, 20L, 99L))
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.getErrorCode()).isEqualTo(ChatErrorCode.INVALID_MESSAGE));

    when(messages.existsByIdAndChatRoomId(10L, 20L)).thenReturn(true);
    when(messages.existsByIdAndChatRoomId(5L, 20L)).thenReturn(true);
    service().markRead(owner, 20L, 10L);
    service().markRead(owner, 20L, 5L);
    assertThat(member.getLastReadMessageId()).isEqualTo(10L);
  }

  @Test
  void deleteCompanionRoomClearsMessagesMembersAndRoom() {
    User owner = user(1L);
    Post post = post(owner, 10L, 2);
    ChatRoom room = room(post, owner, 20L);
    when(rooms.findByCompanionPostId(10L)).thenReturn(Optional.of(room));

    service().deleteCompanionRoom(10L);

    verify(messages).deleteByChatRoomId(20L);
    verify(members).deleteByChatRoomId(20L);
    verify(rooms).delete(room);
  }

  private void assertInvalidMessage(ThrowingCallable action) {
    assertThatThrownBy(action)
        .isInstanceOfSatisfying(BusinessException.class,
            error -> assertThat(error.getErrorCode()).isEqualTo(ChatErrorCode.INVALID_MESSAGE));
  }

  private User user(Long id) {
    User user = UserFixture.activeUser();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private Post post(User owner, Long id, int capacity) {
    Post post = Post.create(owner, PostType.COMPANION, "동행", null, List.of(), null, null,
        LocalDate.now(), capacity, null);
    ReflectionTestUtils.setField(post, "id", id);
    return post;
  }

  private ChatRoom room(Post post, User owner, Long id) {
    ChatRoom room = ChatRoom.createCompanion(post, owner);
    ReflectionTestUtils.setField(room, "id", id);
    return room;
  }

  private Comment comment(Post post, User author, Comment parent, Long id) {
    Comment comment = Comment.create(post, author, parent, "신청합니다");
    ReflectionTestUtils.setField(comment, "id", id);
    return comment;
  }

  private ChatService service() {
    return new ChatService(rooms, members, messages, comments);
  }
}
