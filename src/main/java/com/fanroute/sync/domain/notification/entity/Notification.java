package com.fanroute.sync.domain.notification.entity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private NotificationType type;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, length = 1000)
  private String content;

  @Column(name = "resource_id", nullable = false)
  private Long resourceId;

  @Column(name = "read_at")
  private Instant readAt;

  private Notification(User user, NotificationType type, String title, String content,
      Long resourceId) {
    this.user = user;
    this.type = type;
    this.title = title;
    this.content = content;
    this.resourceId = resourceId;
  }

  public static Notification chatMessage(User user, String senderName, String content, Long roomId) {
    String preview = content.length() > 100 ? content.substring(0, 100) + "…" : content;
    return new Notification(user, NotificationType.CHAT_MESSAGE, senderName + "님의 새 메시지", preview,
        roomId);
  }

  public void markRead(Instant readAt) {
    if (this.readAt == null) this.readAt = readAt;
  }
}
