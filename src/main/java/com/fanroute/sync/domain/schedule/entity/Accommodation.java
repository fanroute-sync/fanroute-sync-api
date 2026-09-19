package com.fanroute.sync.domain.schedule.entity;

import java.time.LocalDate;

import com.fanroute.sync.global.common.entity.BaseTimeEntity;
import com.fanroute.sync.domain.place.entity.Place;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "accommodations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Accommodation extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trip_plan_id", nullable = false)
  private TripPlan tripPlan;

  @Column(name = "name_or_address", nullable = false, length = 300)
  private String nameOrAddress;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id")
  private Place place;

  private Double latitude;

  private Double longitude;

  @Column(name = "checkin_date", nullable = false)
  private LocalDate checkinDate;

  @Column(name = "checkout_date", nullable = false)
  private LocalDate checkoutDate;

  private Accommodation(TripPlan tripPlan, String nameOrAddress, Place place, Double latitude,
      Double longitude, LocalDate checkinDate, LocalDate checkoutDate) {
    this.tripPlan = tripPlan;
    this.nameOrAddress = nameOrAddress;
    this.place = place;
    this.latitude = latitude;
    this.longitude = longitude;
    this.checkinDate = checkinDate;
    this.checkoutDate = checkoutDate;
  }

  public static Accommodation create(TripPlan tripPlan, String nameOrAddress,
      LocalDate checkinDate, LocalDate checkoutDate) {
    return new Accommodation(tripPlan, nameOrAddress, null, null, null, checkinDate, checkoutDate);
  }

  public static Accommodation create(TripPlan tripPlan, String nameOrAddress, Place place,
      Double latitude, Double longitude, LocalDate checkinDate, LocalDate checkoutDate) {
    return new Accommodation(tripPlan, nameOrAddress, place, latitude, longitude, checkinDate,
        checkoutDate);
  }
}
