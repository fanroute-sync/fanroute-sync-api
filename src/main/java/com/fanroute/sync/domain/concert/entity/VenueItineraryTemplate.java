package com.fanroute.sync.domain.concert.entity;

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
@Table(name = "venue_itinerary_templates", uniqueConstraints = {
    @UniqueConstraint(name = "uk_venue_itinerary_templates_venue_name",
        columnNames = {"venue_id", "name"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueItineraryTemplate extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venue_id", nullable = false)
  private Venue venue;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;

  private VenueItineraryTemplate(Venue venue, String name, String description) {
    this.venue = venue;
    this.name = name;
    this.description = description;
  }

  public static VenueItineraryTemplate create(Venue venue, String name, String description) {
    return new VenueItineraryTemplate(venue, name, description);
  }

  public void update(String description) {
    this.description = description;
  }
}
