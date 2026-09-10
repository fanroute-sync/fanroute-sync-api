package com.fanroute.sync.domain.concert.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.support.AbstractRepositoryTest;

class ConcertScheduleRepositoryTest extends AbstractRepositoryTest {

  @Autowired
  private ConcertScheduleRepository concertScheduleRepository;
  @Autowired
  private ConcertRepository concertRepository;
  @Autowired
  private VenueRepository venueRepository;

  private Concert concert() {
    Venue venue = venueRepository.saveAndFlush(
        Venue.create("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0));
    return concertRepository.saveAndFlush(Concert.create(
        "PF001", venue, "테스트 공연", Genre.POPULAR_MUSIC, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 30), "poster.jpg", Instant.now()));
  }

  @Test
  @DisplayName("삭제된 회차는 공연별 목록 조회에 나타나지 않는다")
  void findByConcertIdExcludesSoftDeleted() {
    Concert concert = concert();
    ConcertSchedule active = concertScheduleRepository.saveAndFlush(
        ConcertSchedule.create(concert, 1, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30)));
    ConcertSchedule deleted = concertScheduleRepository.saveAndFlush(
        ConcertSchedule.create(concert, 2, LocalDate.of(2026, 9, 21), LocalTime.of(19, 30)));
    deleted.delete(Instant.now());
    concertScheduleRepository.saveAndFlush(deleted);

    List<ConcertSchedule> found = concertScheduleRepository.findByConcertId(concert.getId());

    assertThat(found).extracting(ConcertSchedule::getId).containsExactly(active.getId());
  }

  @Test
  @DisplayName("삭제된 회차는 ID로 조회해도 나오지 않아 수정·삭제 대상이 될 수 없다")
  void findWithConcertByIdExcludesSoftDeleted() {
    Concert concert = concert();
    ConcertSchedule schedule = concertScheduleRepository.saveAndFlush(
        ConcertSchedule.create(concert, 1, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30)));
    schedule.delete(Instant.now());
    concertScheduleRepository.saveAndFlush(schedule);

    assertThat(concertScheduleRepository.findWithConcertById(schedule.getId())).isEmpty();
  }

  @Test
  @DisplayName("같은 날짜 안 회차만 시각순으로 조회된다 — 재번호(round 계산)에 쓰인다")
  void findByConcertIdAndPerformanceDateOrdersByTime() {
    Concert concert = concert();
    LocalDate date = LocalDate.of(2026, 9, 20);
    concertScheduleRepository.saveAndFlush(
        ConcertSchedule.create(concert, 1, date, LocalTime.of(19, 0)));
    concertScheduleRepository.saveAndFlush(
        ConcertSchedule.create(concert, 1, date, LocalTime.of(16, 0)));
    concertScheduleRepository.saveAndFlush(
        ConcertSchedule.create(concert, 1, LocalDate.of(2026, 9, 21), LocalTime.of(16, 0)));

    List<ConcertSchedule> found =
        concertScheduleRepository.findByConcertIdAndPerformanceDate(concert.getId(), date);

    assertThat(found).extracting(ConcertSchedule::getPerformanceTime)
        .containsExactly(LocalTime.of(16, 0), LocalTime.of(19, 0));
  }

  // (concert_id, performance_date, performance_time) 유니크 제약이 부분 유니크 인덱스
  // (WHERE deleted_at IS NULL)라, 삭제된 회차의 시각은 재사용 가능해야 합니다 — 이 동작은 실제 DB
  // (Testcontainers Postgres)로만 검증됩니다.
  @Test
  @DisplayName("삭제된 회차의 날짜·시각은 다른 회차가 재사용할 수 있다")
  void allowsReusingDateTimeAfterSoftDelete() {
    Concert concert = concert();
    LocalDate date = LocalDate.of(2026, 9, 20);
    LocalTime time = LocalTime.of(19, 30);
    ConcertSchedule original = concertScheduleRepository.saveAndFlush(
        ConcertSchedule.create(concert, 1, date, time));
    original.delete(Instant.now());
    concertScheduleRepository.saveAndFlush(original);

    ConcertSchedule reused = concertScheduleRepository.saveAndFlush(
        ConcertSchedule.create(concert, 1, date, time));

    assertThat(reused.getId()).isNotEqualTo(original.getId());
  }

  @Test
  @DisplayName("같은 날짜에 같은 회차 번호라도 시각이 다르면 등록할 수 있다")
  void allowsSameRoundOnSameDateWithDifferentTime() {
    Concert concert = concert();
    LocalDate date = LocalDate.of(2026, 9, 20);
    concertScheduleRepository.saveAndFlush(
        ConcertSchedule.create(concert, 1, date, LocalTime.of(16, 0)));

    ConcertSchedule second = concertScheduleRepository.saveAndFlush(
        ConcertSchedule.create(concert, 1, date, LocalTime.of(19, 0)));

    assertThat(second.getId()).isNotNull();
  }

  @Test
  @DisplayName("삭제되지 않은 회차끼리는 같은 날짜·시각을 중복 등록할 수 없다")
  void rejectsDuplicateDateTimeAmongActiveSchedules() {
    Concert concert = concert();
    LocalDate date = LocalDate.of(2026, 9, 20);
    LocalTime time = LocalTime.of(19, 30);
    concertScheduleRepository.saveAndFlush(ConcertSchedule.create(concert, 1, date, time));

    assertThatThrownBy(() -> concertScheduleRepository.saveAndFlush(
        ConcertSchedule.create(concert, 2, date, time)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
