package com.fanroute.sync.domain.concert.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConcertScheduleEntityTest {

  private Concert concert() {
    Venue venue = Venue.create("MT10TEST", "테스트 공연장", "부산광역시", 35.1, 129.1);
    return Concert.create(
        "MT20TEST", venue, "테스트 콘서트", Genre.POPULAR_MUSIC,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null,
        Instant.parse("2026-08-01T00:00:00Z"));
  }

  @Test
  @DisplayName("관리자가 회차를 직접 생성하면 처음부터 확정 상태다")
  void createsConcertSchedule() {
    Concert concert = concert();

    ConcertSchedule schedule =
        ConcertSchedule.create(concert, 1, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));

    assertThat(schedule.getConcert()).isSameAs(concert);
    assertThat(schedule.getRound()).isEqualTo(1);
    assertThat(schedule.getPerformanceDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    assertThat(schedule.getPerformanceTime()).isEqualTo(LocalTime.of(19, 30));
    assertThat(schedule.isProvisional()).isFalse();
    assertThat(schedule.getSource()).isEqualTo(ScheduleSource.MANUAL);
  }

  @Test
  @DisplayName("KOPIS 안내 파싱 결과로 생성한 회차는 잠정 상태로 시작한다")
  void createsScheduleFromParsedSlot() {
    Concert concert = concert();

    ConcertSchedule schedule = ConcertSchedule.createFromParsedSlot(
        concert, 1, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));

    assertThat(schedule.isProvisional()).isTrue();
    assertThat(schedule.getSource()).isEqualTo(ScheduleSource.KOPIS_PARSED);
  }

  @Test
  @DisplayName("회차를 수정하면 날짜·시간이 갱신되고 확정(MANUAL) 상태로 바뀐다")
  void updatesSchedule() {
    Concert concert = concert();
    ConcertSchedule schedule = ConcertSchedule.createFromParsedSlot(
        concert, 1, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));

    schedule.update(LocalDate.of(2026, 9, 21), LocalTime.of(20, 0));

    assertThat(schedule.getPerformanceDate()).isEqualTo(LocalDate.of(2026, 9, 21));
    assertThat(schedule.getPerformanceTime()).isEqualTo(LocalTime.of(20, 0));
    assertThat(schedule.isProvisional()).isFalse();
    assertThat(schedule.getSource()).isEqualTo(ScheduleSource.MANUAL);
  }

  @Test
  @DisplayName("같은 날짜의 회차들은 시각순으로 1부터 다시 번호가 매겨진다")
  void renumbersByTime() {
    Concert concert = concert();
    LocalDate date = LocalDate.of(2026, 9, 5);
    ConcertSchedule evening = ConcertSchedule.create(concert, 99, date, LocalTime.of(19, 0));
    ConcertSchedule afternoon = ConcertSchedule.create(concert, 1, date, LocalTime.of(16, 0));

    ConcertSchedule.renumberByTime(List.of(evening, afternoon));

    assertThat(afternoon.getRound()).isEqualTo(1);
    assertThat(evening.getRound()).isEqualTo(2);
  }

  @Test
  @DisplayName("삭제하면 하드 삭제 없이 deletedAt만 기록된다")
  void deletesSoftly() {
    ConcertSchedule schedule =
        ConcertSchedule.create(concert(), 1, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));
    Instant deletedAt = Instant.parse("2026-09-06T00:00:00Z");

    schedule.delete(deletedAt);

    assertThat(schedule.isDeleted()).isTrue();
    assertThat(schedule.getDeletedAt()).isEqualTo(deletedAt);
  }
}
