package com.fanroute.sync.domain.concert.entity;

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
@Table(name = "venue_place_collections", uniqueConstraints = {
    @UniqueConstraint(name = "uk_venue_place_collections_venue_name",
        columnNames = {"venue_id", "name"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenuePlaceCollection extends BaseTimeEntity {

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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private PlaceCollectionType type;

  private VenuePlaceCollection(Venue venue, String name, String description,
      PlaceCollectionType type) {
    this.venue = venue;
    this.name = name;
    this.description = description;
    this.type = type;
  }

  public static VenuePlaceCollection create(Venue venue, String name, String description,
      PlaceCollectionType type) {
    return new VenuePlaceCollection(venue, name, description, type);
  }

  public void update(String description, PlaceCollectionType type) {
    this.description = description;
    this.type = type;
  }
}
