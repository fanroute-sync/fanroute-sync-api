package com.fanroute.sync.domain.concert.entity;

import com.fanroute.sync.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "venues", uniqueConstraints = {
    @UniqueConstraint(name = "uk_venues_kopis_venue_id", columnNames = "kopis_venue_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venue extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "kopis_venue_id", nullable = false, length = 20)
  private String kopisVenueId; // KOPIS 공연시설 ID(mt10id)

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 300)
  private String address;

  private Double latitude;

  private Double longitude;

  private Venue(String kopisVenueId, String name, String address, Double latitude,
      Double longitude) {
    this.kopisVenueId = kopisVenueId;
    this.name = name;
    this.address = address;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public static Venue create(
      String kopisVenueId, String name, String address, Double latitude, Double longitude) {
    return new Venue(kopisVenueId, name, address, latitude, longitude);
  }

  public void updateFromSync(String name, String address, Double latitude, Double longitude) {
    this.name = name;
    this.address = address;
    this.latitude = latitude;
    this.longitude = longitude;
  }
}
