package com.fanroute.sync.domain.schedule.entity;

import java.time.LocalTime;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.place.entity.Place;
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
@Table(name = "itinerary_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItineraryItem extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "itinerary_day_id", nullable = false)
  private ItineraryDay itineraryDay;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "scheduled_time")
  private LocalTime scheduledTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ItineraryItemType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id")
  private Place place;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "concert_id")
  private Concert concert;

  @Column(length = 200)
  private String title;

  @Column(name = "duration_minutes")
  private Integer durationMinutes;

  private ItineraryItem(ItineraryDay itineraryDay, int sortOrder, LocalTime scheduledTime,
      ItineraryItemType type, Place place, Concert concert, String title,
      Integer durationMinutes) {
    this.itineraryDay = itineraryDay;
    this.sortOrder = sortOrder;
    this.scheduledTime = scheduledTime;
    this.type = type;
    this.place = place;
    this.concert = concert;
    this.title = title;
    this.durationMinutes = durationMinutes;
  }

  public static ItineraryItem create(ItineraryDay itineraryDay, int sortOrder,
      LocalTime scheduledTime, ItineraryItemType type, Place place, Concert concert,
      String title, Integer durationMinutes) {
    return new ItineraryItem(
        itineraryDay, sortOrder, scheduledTime, type, place, concert, title, durationMinutes);
  }

  public void update(LocalTime scheduledTime, String title, Integer durationMinutes, Place place) {
    this.scheduledTime = scheduledTime;
    this.title = title;
    this.durationMinutes = durationMinutes;
    this.place = place;
  }

  public void changeSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }
}
