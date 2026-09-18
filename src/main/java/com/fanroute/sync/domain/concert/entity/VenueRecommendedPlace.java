package com.fanroute.sync.domain.concert.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "venue_recommended_places", uniqueConstraints = {
    @UniqueConstraint(name = "uk_venue_recommended_places_venue_place",
        columnNames = {"venue_id", "place_id"}),
    @UniqueConstraint(name = "uk_venue_recommended_places_venue_sort_order",
        columnNames = {"venue_id", "sort_order"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueRecommendedPlace extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venue_id", nullable = false)
  private Venue venue;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Enumerated(EnumType.STRING)
  @Column(name = "recommended_time_slot", length = 20)
  private ConcertTimeSlot recommendedTimeSlot;

  private VenueRecommendedPlace(
      Venue venue, Place place, int sortOrder, ConcertTimeSlot recommendedTimeSlot) {
    this.venue = venue;
    this.place = place;
    this.sortOrder = sortOrder;
    this.recommendedTimeSlot = recommendedTimeSlot;
  }

  public static VenueRecommendedPlace create(
      Venue venue, Place place, int sortOrder, ConcertTimeSlot recommendedTimeSlot) {
    return new VenueRecommendedPlace(venue, place, sortOrder, recommendedTimeSlot);
  }

  public void update(Place place, int sortOrder, ConcertTimeSlot recommendedTimeSlot) {
    this.place = place;
    this.sortOrder = sortOrder;
    this.recommendedTimeSlot = recommendedTimeSlot;
  }
}
