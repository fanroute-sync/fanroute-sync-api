package com.fanroute.sync.domain.concert.entity;

import java.time.Instant;
import java.time.LocalDate;

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
  private Genre genreName;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "poster_url", length = 500)
  private String posterUrl;

  @Column(name = "performance_time_guide", columnDefinition = "TEXT")
  private String performanceTimeGuide;

  @Enumerated(EnumType.STRING)
  @Column(name = "schedule_parse_status", nullable = false, length = 20)
  private ScheduleParseStatus scheduleParseStatus = ScheduleParseStatus.NOT_PARSED;

  @Column(name = "schedule_parser_version")
  private Integer scheduleParserVersion;

  @Column(name = "schedule_source_hash", length = 64)
  private String scheduleSourceHash;

  @Column(name = "last_synced_at", nullable = false)
  private Instant lastSyncedAt;

  private Concert(String kopisConcertId, Venue venue, String title, Genre genreName,
      LocalDate startDate, LocalDate endDate, String posterUrl, String performanceTimeGuide,
      Instant lastSyncedAt) {
    this.kopisConcertId = kopisConcertId;
    this.venue = venue;
    this.title = title;
    this.genreName = genreName;
    this.startDate = startDate;
    this.endDate = endDate;
    this.posterUrl = posterUrl;
    this.performanceTimeGuide = performanceTimeGuide;
    this.lastSyncedAt = lastSyncedAt;
  }

  public static Concert create(String kopisConcertId, Venue venue, String title,
      Genre genreName, LocalDate startDate, LocalDate endDate, String posterUrl,
      Instant lastSyncedAt) {
    return create(
        kopisConcertId, venue, title, genreName, startDate, endDate, posterUrl, null,
        lastSyncedAt);
  }

  public static Concert create(String kopisConcertId, Venue venue, String title,
      Genre genreName, LocalDate startDate, LocalDate endDate, String posterUrl,
      String performanceTimeGuide, Instant lastSyncedAt) {
    return new Concert(
        kopisConcertId, venue, title, genreName, startDate, endDate, posterUrl,
        performanceTimeGuide, lastSyncedAt);
  }

  // 기존 호출 경로는 시간 안내를 지우지 않도록 현재 값을 유지합니다.
  public void updateFromSync(Venue venue, String title, Genre genreName, LocalDate startDate,
      LocalDate endDate, String posterUrl, Instant syncedAt) {
    updateFromSync(
        venue, title, genreName, startDate, endDate, posterUrl, performanceTimeGuide, syncedAt);
  }

  public void updateFromSync(Venue venue, String title, Genre genreName, LocalDate startDate,
      LocalDate endDate, String posterUrl, String performanceTimeGuide, Instant syncedAt) {
    this.venue = venue;
    this.title = title;
    this.genreName = genreName;
    this.startDate = startDate;
    this.endDate = endDate;
    this.posterUrl = posterUrl;
    this.performanceTimeGuide = performanceTimeGuide;
    this.lastSyncedAt = syncedAt;
  }

  public void updateScheduleParseState(ScheduleParseStatus status, int parserVersion,
      String sourceHash) {
    this.scheduleParseStatus = status;
    this.scheduleParserVersion = parserVersion;
    this.scheduleSourceHash = sourceHash;
  }

  public boolean needsScheduleReparse(String currentHash, int currentParserVersion) {
    return scheduleParserVersion == null
        || scheduleParserVersion != currentParserVersion
        || !currentHash.equals(scheduleSourceHash);
  }
}
