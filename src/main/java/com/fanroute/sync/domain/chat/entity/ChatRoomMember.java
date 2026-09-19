package com.fanroute.sync.domain.chat.entity;

import java.time.Instant;

import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_room_members", uniqueConstraints = @UniqueConstraint(
    name = "uk_chat_room_members_room_user", columnNames = {"chat_room_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember extends BaseTimeEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_room_id", nullable = false)
  private ChatRoom chatRoom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ChatMemberRole role;

  @Column(name = "last_read_message_id")
  private Long lastReadMessageId;

  @Column(name = "last_read_at")
  private Instant lastReadAt;

  @Column(name = "left_at")
  private Instant leftAt;

  private ChatRoomMember(ChatRoom chatRoom, User user, ChatMemberRole role) {
    this.chatRoom = chatRoom;
    this.user = user;
    this.role = role;
  }

  public static ChatRoomMember create(ChatRoom chatRoom, User user, ChatMemberRole role) {
    return new ChatRoomMember(chatRoom, user, role);
  }

  public boolean isActive() { return leftAt == null; }

  public void rejoin() { this.leftAt = null; }

  public void markRead(Long messageId, Instant readAt) {
    if (lastReadMessageId == null || messageId > lastReadMessageId) {
      this.lastReadMessageId = messageId;
      this.lastReadAt = readAt;
    }
  }
}
