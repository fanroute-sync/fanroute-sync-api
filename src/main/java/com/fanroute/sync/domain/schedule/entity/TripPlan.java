package com.fanroute.sync.domain.schedule.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.entity.BaseTimeEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "trip_plans")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripPlan extends BaseTimeEntity {

  public static final int AI_GENERATION_LIMIT = 5;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "concert_id")
  private Concert concert;

  @Column(name = "arrival_at", nullable = false)
  private Instant arrivalAt;

  @Column(name = "departure_at", nullable = false)
  private Instant departureAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "travel_intensity", length = 20)
  private TravelIntensityType travelIntensity;

  @ElementCollection
  @CollectionTable(name = "trip_plan_companions", joinColumns = @JoinColumn(name = "trip_plan_id"))
  @OrderColumn(name = "display_order")
  @Column(name = "companion", nullable = false, length = 30)
  private List<String> companions = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "trip_plan_preferences", joinColumns = @JoinColumn(name = "trip_plan_id"))
  @OrderColumn(name = "display_order")
  @Column(name = "preference", nullable = false, length = 50)
  private List<String> preferences = new ArrayList<>();

  @Column(name = "ai_generation_used_count", nullable = false)
  private int aiGenerationUsedCount;

  @Column(name = "ai_generation_reserved_count", nullable = false)
  private int aiGenerationReservedCount;

  private TripPlan(User user, Concert concert, Instant arrivalAt, Instant departureAt,
      TravelIntensityType travelIntensity, List<String> companions, List<String> preferences) {
    this.user = user;
    this.concert = concert;
    this.arrivalAt = arrivalAt;
    this.departureAt = departureAt;
    this.travelIntensity = travelIntensity;
    this.companions = new ArrayList<>(companions);
    this.preferences = new ArrayList<>(preferences);
  }

  public static TripPlan create(User user, Concert concert, Instant arrivalAt, Instant departureAt,
      TravelIntensityType travelIntensity, List<String> companions, List<String> preferences) {
    return new TripPlan(
        user, concert, arrivalAt, departureAt, travelIntensity, companions, preferences);
  }
}
