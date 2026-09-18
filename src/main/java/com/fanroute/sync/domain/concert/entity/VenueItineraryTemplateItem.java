package com.fanroute.sync.domain.concert.entity;

import java.time.LocalTime;

import com.fanroute.sync.domain.place.entity.Place;
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
@Table(name = "venue_itinerary_template_items", uniqueConstraints = {
    @UniqueConstraint(name = "uk_template_items_template_place",
        columnNames = {"template_id", "place_id"}),
    @UniqueConstraint(name = "uk_template_items_template_sort_order",
        columnNames = {"template_id", "sort_order"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueItineraryTemplateItem extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", nullable = false)
  private VenueItineraryTemplate template;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  private LocalTime defaultTime;

  private Integer defaultDurationMinutes;

  private VenueItineraryTemplateItem(VenueItineraryTemplate template, Place place, int sortOrder,
      LocalTime defaultTime, Integer defaultDurationMinutes) {
    this.template = template;
    this.place = place;
    this.sortOrder = sortOrder;
    this.defaultTime = defaultTime;
    this.defaultDurationMinutes = defaultDurationMinutes;
  }

  public static VenueItineraryTemplateItem create(VenueItineraryTemplate template, Place place,
      int sortOrder, LocalTime defaultTime, Integer defaultDurationMinutes) {
    return new VenueItineraryTemplateItem(template, place, sortOrder, defaultTime,
        defaultDurationMinutes);
  }
}
