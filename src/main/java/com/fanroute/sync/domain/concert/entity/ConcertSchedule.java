package com.fanroute.sync.domain.concert.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import com.fanroute.sync.global.common.entity.SoftDeleteEntity;

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

/** 공연 하나의 개별 회차입니다. round는 같은 performanceDate 안에서만 유일하며, 실제 유일성은
 * schema.sql의 부분 유니크 인덱스가 보장합니다. */
@Getter
@Entity
@Table(name = "concert_schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcertSchedule extends SoftDeleteEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "concert_id", nullable = false)
  private Concert concert;

  @Column(nullable = false)
  private int round;

  @Column(name = "performance_date", nullable = false)
  private LocalDate performanceDate;

  @Column(name = "performance_time", nullable = false)
  private LocalTime performanceTime;

  @Column(name = "is_provisional", nullable = false)
  private boolean provisional;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ScheduleSource source;

  private ConcertSchedule(Concert concert, int round, LocalDate performanceDate,
      LocalTime performanceTime, boolean provisional, ScheduleSource source) {
    this.concert = concert;
    this.round = round;
    this.performanceDate = performanceDate;
    this.performanceTime = performanceTime;
    this.provisional = provisional;
    this.source = source;
  }

  /** round는 호출자가 같은 날짜 안 시각순으로 계산해 넘겨야 합니다({@link #renumberByTime}). */
  public static ConcertSchedule create(
      Concert concert, int round, LocalDate performanceDate, LocalTime performanceTime) {
    return new ConcertSchedule(
        concert, round, performanceDate, performanceTime, false, ScheduleSource.MANUAL);
  }

  public static ConcertSchedule createFromParsedSlot(
      Concert concert, int round, LocalDate performanceDate, LocalTime performanceTime) {
    return new ConcertSchedule(
        concert, round, performanceDate, performanceTime, true, ScheduleSource.KOPIS_PARSED);
  }

  public void update(LocalDate performanceDate, LocalTime performanceTime) {
    this.performanceDate = performanceDate;
    this.performanceTime = performanceTime;
    this.provisional = false;
    this.source = ScheduleSource.MANUAL;
  }

  void assignRound(int round) {
    this.round = round;
  }

  /** 같은 날짜의 회차들을 시각순으로 1..N 재번호합니다. 대상 목록은 호출자가 모아 넘깁니다. */
  public static void renumberByTime(List<ConcertSchedule> schedulesOnSameDate) {
    List<ConcertSchedule> sorted = schedulesOnSameDate.stream()
        .sorted(Comparator.comparing(ConcertSchedule::getPerformanceTime))
        .toList();
    for (int i = 0; i < sorted.size(); i++) {
      sorted.get(i).assignRound(i + 1);
    }
  }

  public void delete(Instant deletedAt) {
    markDeleted(deletedAt);
  }
}
