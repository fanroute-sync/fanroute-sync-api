package com.fanroute.sync.domain.schedule.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_generation_notification_outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiGenerationNotificationOutboxEvent extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "generation_id", nullable = false, unique = true)
  private AiItineraryGeneration generation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AiGenerationNotificationType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AiGenerationNotificationStatus status;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(name = "processing_lease_until")
  private Instant processingLeaseUntil;

  @Column(name = "last_failure_reason", length = 1000)
  private String lastFailureReason;

  private AiGenerationNotificationOutboxEvent(AiItineraryGeneration generation, User user,
      AiGenerationNotificationType type, Instant now) {
    this.generation = generation;
    this.user = user;
    this.type = type;
    this.status = AiGenerationNotificationStatus.PENDING;
    this.nextAttemptAt = now;
  }

  public static AiGenerationNotificationOutboxEvent create(AiItineraryGeneration generation,
      User user, AiGenerationNotificationType type, Instant now) {
    return new AiGenerationNotificationOutboxEvent(generation, user, type, now);
  }

  public void claim(Instant leaseUntil) {
    status = AiGenerationNotificationStatus.PROCESSING;
    processingLeaseUntil = leaseUntil;
    attemptCount++;
  }

  public boolean sent(int claimedAttemptCount) {
    if (!isCurrentClaim(claimedAttemptCount)) {
      return false;
    }
    status = AiGenerationNotificationStatus.SENT;
    processingLeaseUntil = null;
    return true;
  }

  public boolean retry(int claimedAttemptCount, Instant nextAttemptAt, String failureReason) {
    if (!isCurrentClaim(claimedAttemptCount)) {
      return false;
    }
    status = AiGenerationNotificationStatus.PENDING;
    this.nextAttemptAt = nextAttemptAt;
    processingLeaseUntil = null;
    lastFailureReason = failureReason;
    return true;
  }

  public boolean fail(int claimedAttemptCount, String failureReason) {
    if (!isCurrentClaim(claimedAttemptCount)) {
      return false;
    }
    status = AiGenerationNotificationStatus.FAILED;
    processingLeaseUntil = null;
    lastFailureReason = failureReason;
    return true;
  }

  private boolean isCurrentClaim(int claimedAttemptCount) {
    return status == AiGenerationNotificationStatus.PROCESSING
        && attemptCount == claimedAttemptCount;
  }
}
