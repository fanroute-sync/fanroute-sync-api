package com.fanroute.sync.domain.schedule.entity;

import java.time.Instant;

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
@Table(name = "ai_itinerary_generations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiItineraryGeneration extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "itinerary_day_id", nullable = false)
  private ItineraryDay itineraryDay;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AiItineraryGenerationStatus status;

  @Column(name = "processing_lease_until")
  private Instant processingLeaseUntil;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;

  @Column(name = "last_failure_reason", length = 1000)
  private String lastFailureReason;

  private AiItineraryGeneration(ItineraryDay itineraryDay) {
    this.itineraryDay = itineraryDay;
    this.status = AiItineraryGenerationStatus.PENDING;
    this.attemptCount = 0;
  }

  public static AiItineraryGeneration create(ItineraryDay itineraryDay) {
    return new AiItineraryGeneration(itineraryDay);
  }

  public void cancel() {
    this.status = AiItineraryGenerationStatus.CANCELLED;
    this.processingLeaseUntil = null;
    this.nextAttemptAt = null;
  }

  public void start(Instant processingLeaseUntil) {
    this.status = AiItineraryGenerationStatus.PROCESSING;
    this.processingLeaseUntil = processingLeaseUntil;
  }

  public void complete() {
    this.status = AiItineraryGenerationStatus.COMPLETED;
    this.processingLeaseUntil = null;
    this.nextAttemptAt = null;
  }

  public void fail() {
    fail(null);
  }

  public void fail(String failureReason) {
    this.status = AiItineraryGenerationStatus.FAILED;
    this.processingLeaseUntil = null;
    this.nextAttemptAt = null;
    this.lastFailureReason = failureReason;
  }

  public void scheduleRetry(Instant nextAttemptAt, String failureReason) {
    this.status = AiItineraryGenerationStatus.PENDING;
    this.processingLeaseUntil = null;
    this.nextAttemptAt = nextAttemptAt;
    this.lastFailureReason = failureReason;
  }
}
