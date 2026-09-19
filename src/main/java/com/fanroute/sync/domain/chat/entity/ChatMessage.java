package com.fanroute.sync.domain.chat.entity;

import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.entity.SoftDeleteEntity;

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
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends SoftDeleteEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_room_id", nullable = false)
  private ChatRoom chatRoom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @Enumerated(EnumType.STRING)
  @Column(name = "message_type", nullable = false, length = 20)
  private ChatMessageType messageType;

  @Column(columnDefinition = "TEXT")
  private String content;

  private ChatMessage(ChatRoom chatRoom, User sender, String content) {
    this.chatRoom = chatRoom;
    this.sender = sender;
    this.messageType = ChatMessageType.TEXT;
    this.content = content;
  }

  public static ChatMessage createText(ChatRoom chatRoom, User sender, String content) {
    return new ChatMessage(chatRoom, sender, content);
  }
}
