package com.fanroute.sync.domain.schedule.entity;

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

  private AiItineraryGeneration(ItineraryDay itineraryDay) {
    this.itineraryDay = itineraryDay;
    this.status = AiItineraryGenerationStatus.PENDING;
  }

  public static AiItineraryGeneration create(ItineraryDay itineraryDay) {
    return new AiItineraryGeneration(itineraryDay);
  }
}
