package com.fanroute.sync.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.chat.entity.ChatRoom;
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

  private User user(Long id) {
    User user = UserFixture.activeUser();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  private ChatService service() {
    return new ChatService(rooms, members, messages, comments);
  }
}
