package com.fanroute.sync.domain.schedule.entity;

import com.fanroute.sync.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_generation_dead_letters")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiGenerationDeadLetter extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "generation_id", nullable = false, unique = true)
  private AiItineraryGeneration generation;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(nullable = false, length = 1000)
  private String reason;

  private AiGenerationDeadLetter(AiItineraryGeneration generation, int attemptCount,
      String reason) {
    this.generation = generation;
    this.attemptCount = attemptCount;
    this.reason = reason;
  }

  public static AiGenerationDeadLetter create(AiItineraryGeneration generation, String reason) {
    return new AiGenerationDeadLetter(generation, generation.getAttemptCount(), reason);
  }
}
