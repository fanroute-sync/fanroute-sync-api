package com.fanroute.sync.domain.schedule.entity;

import java.time.LocalDate;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "itinerary_days", uniqueConstraints = {
    @UniqueConstraint(name = "uk_itinerary_days_trip_plan_id_date", columnNames = {
        "trip_plan_id", "date"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItineraryDay extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_plan_id", nullable = false)
  private TripPlan tripPlan;

  @Column(nullable = false)
  private LocalDate date;

  @Column(name = "is_concert_day", nullable = false)
  private boolean concertDay;

  private ItineraryDay(TripPlan tripPlan, LocalDate date, boolean concertDay) {
    this.tripPlan = tripPlan;
    this.date = date;
    this.concertDay = concertDay;
  }

  public static ItineraryDay create(TripPlan tripPlan, LocalDate date, boolean concertDay) {
    return new ItineraryDay(tripPlan, date, concertDay);
  }
}
