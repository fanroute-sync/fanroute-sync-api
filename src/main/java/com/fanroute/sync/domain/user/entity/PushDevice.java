package com.fanroute.sync.domain.user.entity;

import java.time.Instant;

import com.fanroute.sync.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "push_devices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushDevice extends BaseTimeEntity {

  private static final int MAX_TOKEN_LENGTH = 4096;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "registration_token", nullable = false, unique = true, length = MAX_TOKEN_LENGTH)
  private String registrationToken;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "last_seen_at", nullable = false)
  private Instant lastSeenAt;

  private PushDevice(User user, String registrationToken, Instant now) {
    this.user = user;
    this.registrationToken = registrationToken;
    this.enabled = true;
    this.lastSeenAt = now;
  }

  public static PushDevice create(User user, String registrationToken, Instant now) {
    return new PushDevice(user, registrationToken, now);
  }

  public void refresh(User user, Instant now) {
    this.user = user;
    this.enabled = true;
    this.lastSeenAt = now;
  }

  public void disable() {
    this.enabled = false;
  }
}
