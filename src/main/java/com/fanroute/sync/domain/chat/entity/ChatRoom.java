package com.fanroute.sync.domain.chat.entity;

import java.time.Instant;

import com.fanroute.sync.domain.community.entity.Post;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ChatRoomType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "companion_post_id")
  private Post companionPost;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Column(name = "last_message_id")
  private Long lastMessageId;

  @Column(name = "last_message_at")
  private Instant lastMessageAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  private ChatRoom(Post companionPost, User createdBy) {
    this.type = ChatRoomType.COMPANION;
    this.companionPost = companionPost;
    this.createdBy = createdBy;
  }

  public static ChatRoom createCompanion(Post companionPost, User createdBy) {
    return new ChatRoom(companionPost, createdBy);
  }

  public void updateLastMessage(Long messageId, Instant sentAt) {
    this.lastMessageId = messageId;
    this.lastMessageAt = sentAt;
  }
}
