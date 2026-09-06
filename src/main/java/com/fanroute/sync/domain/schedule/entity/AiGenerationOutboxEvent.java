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
@Table(name = "ai_generation_outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiGenerationOutboxEvent extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "generation_id", nullable = false)
  private AiItineraryGeneration generation;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AiGenerationOutboxStatus status;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(name = "publish_lease_until")
  private Instant publishLeaseUntil;

  private AiGenerationOutboxEvent(AiItineraryGeneration generation, Instant now) {
    this.generation = generation;
    this.status = AiGenerationOutboxStatus.PENDING;
    this.nextAttemptAt = now;
  }

  public static AiGenerationOutboxEvent create(AiItineraryGeneration generation, Instant now) {
    return new AiGenerationOutboxEvent(generation, now);
  }

  public void claim(Instant leaseUntil) {
    this.status = AiGenerationOutboxStatus.PUBLISHING;
    this.publishLeaseUntil = leaseUntil;
  }

  public void publish() {
    this.status = AiGenerationOutboxStatus.PUBLISHED;
    this.publishLeaseUntil = null;
  }

  public void reschedule(Instant nextAttemptAt) {
    this.status = AiGenerationOutboxStatus.PENDING;
    this.nextAttemptAt = nextAttemptAt;
    this.publishLeaseUntil = null;
  }
}
