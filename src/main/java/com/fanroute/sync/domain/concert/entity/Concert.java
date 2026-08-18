package com.fanroute.sync.domain.concert.entity;

import java.time.Instant;
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
@Table(name = "concerts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_concerts_kopis_concert_id", columnNames = "kopis_concert_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Concert extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "kopis_concert_id", nullable = false, length = 20)
  private String kopisConcertId; // KOPIS 공연 ID(mt20id)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venue_id", nullable = false)
  private Venue venue;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "genre_name", length = 50)
  private String genreName;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "poster_url", length = 500)
  private String posterUrl;

  @Column(name = "last_synced_at", nullable = false)
  private Instant lastSyncedAt;

  private Concert(String kopisConcertId, Venue venue, String title, String genreName,
      LocalDate startDate, LocalDate endDate, String posterUrl, Instant lastSyncedAt) {
    this.kopisConcertId = kopisConcertId;
    this.venue = venue;
    this.title = title;
    this.genreName = genreName;
    this.startDate = startDate;
    this.endDate = endDate;
    this.posterUrl = posterUrl;
    this.lastSyncedAt = lastSyncedAt;
  }

  public static Concert create(String kopisConcertId, Venue venue, String title,
      String genreName, LocalDate startDate, LocalDate endDate, String posterUrl,
      Instant lastSyncedAt) {
    return new Concert(
        kopisConcertId, venue, title, genreName, startDate, endDate, posterUrl, lastSyncedAt);
  }

  public void updateFromSync(Venue venue, String title, String genreName, LocalDate startDate,
      LocalDate endDate, String posterUrl, Instant syncedAt) {
    this.venue = venue;
    this.title = title;
    this.genreName = genreName;
    this.startDate = startDate;
    this.endDate = endDate;
    this.posterUrl = posterUrl;
    this.lastSyncedAt = syncedAt;
  }
}
