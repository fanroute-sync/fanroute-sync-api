package com.fanroute.sync.domain.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.fanroute.sync.domain.concert.dto.ConcertScheduleDto;
import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.entity.ScheduleSource;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.ConcertScheduleRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ConcertScheduleServiceTest {

  @Mock
  private ConcertScheduleRepository concertScheduleRepository;
  @Mock
  private ConcertService concertService;

  private ConcertScheduleService concertScheduleService;
  private Clock clock;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-09-06T00:00:00Z"), ZoneOffset.UTC);
    concertScheduleService =
        new ConcertScheduleService(concertScheduleRepository, concertService, clock);
  }

  private Concert concert() {
    Venue venue = Venue.create("FC001", "테스트홀", "부산", 35.1, 129.0);
    return Concert.create(
        "PF001", venue, "테스트 공연", Genre.POPULAR_MUSIC, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 30), "poster.jpg", Instant.now());
  }

  @Test
  @DisplayName("존재하는 공연으로 회차를 생성하면 확정(MANUAL) 상태로 만들어진다")
  void createsScheduleForExistingConcert() {
    Concert concert = concert();
    when(concertService.getConcert(1L)).thenReturn(concert);
    when(concertScheduleRepository.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ConcertScheduleDto.CreateRequest request = new ConcertScheduleDto.CreateRequest(
        1L, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));

    ConcertSchedule result = concertScheduleService.createSchedule(request);

    assertThat(result.getPerformanceDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    assertThat(result.getPerformanceTime()).isEqualTo(LocalTime.of(19, 30));
    assertThat(result.isProvisional()).isFalse();
    assertThat(result.getSource()).isEqualTo(ScheduleSource.MANUAL);
  }

  @Test
  @DisplayName("회차를 생성하면 그 날짜의 회차 목록을 다시 조회해 시각순으로 재번호한다")
  void renumbersSiblingsAfterCreate() {
    Concert concert = concert();
    LocalDate date = LocalDate.of(2026, 9, 20);
    when(concertService.getConcert(1L)).thenReturn(concert);
    when(concertScheduleRepository.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    // 저장 후 재조회 결과를 흉내 낸 목록 — 새 회차(19:30)가 기존 회차(16:00)보다 늦은 시각이다.
    ConcertSchedule earlierSibling = ConcertSchedule.create(concert, 5, date, LocalTime.of(16, 0));
    ConcertSchedule laterSibling = ConcertSchedule.create(concert, 3, date, LocalTime.of(19, 30));
    when(concertScheduleRepository.findByConcertIdAndPerformanceDate(any(), eq(date)))
        .thenReturn(List.of(earlierSibling, laterSibling));

    concertScheduleService.createSchedule(
        new ConcertScheduleDto.CreateRequest(1L, date, LocalTime.of(19, 30)));

    assertThat(earlierSibling.getRound()).isEqualTo(1);
    assertThat(laterSibling.getRound()).isEqualTo(2);
  }

  @Test
  @DisplayName("존재하지 않는 공연으로 회차를 생성하면 예외가 발생한다")
  void throwsWhenConcertNotFoundOnCreate() {
    when(concertService.getConcert(1L))
        .thenThrow(new BusinessException(ConcertErrorCode.CONCERT_NOT_FOUND));
    ConcertScheduleDto.CreateRequest request = new ConcertScheduleDto.CreateRequest(
        1L, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));

    assertThatThrownBy(() -> concertScheduleService.createSchedule(request))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.CONCERT_NOT_FOUND));
  }

  @Test
  @DisplayName("공연 기간 밖의 날짜로 회차를 생성하면 예외가 발생한다")
  void throwsWhenPerformanceDateOutsideConcertPeriod() {
    when(concertService.getConcert(1L)).thenReturn(concert());
    ConcertScheduleDto.CreateRequest request = new ConcertScheduleDto.CreateRequest(
        1L, LocalDate.of(2026, 10, 1), LocalTime.of(19, 30));

    assertThatThrownBy(() -> concertScheduleService.createSchedule(request))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.SCHEDULE_DATE_OUT_OF_RANGE));
  }

  @Test
  @DisplayName("같은 공연에 같은 날짜·시각을 중복 등록하면 예외가 발생한다")
  void throwsWhenDateTimeDuplicated() {
    when(concertService.getConcert(1L)).thenReturn(concert());
    when(concertScheduleRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate"));
    ConcertScheduleDto.CreateRequest request = new ConcertScheduleDto.CreateRequest(
        1L, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));

    assertThatThrownBy(() -> concertScheduleService.createSchedule(request))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.DUPLICATE_SCHEDULE_TIME));
  }

  @Test
  @DisplayName("존재하지 않는 회차를 수정하면 예외가 발생한다")
  void throwsWhenScheduleNotFoundOnUpdate() {
    when(concertScheduleRepository.findWithConcertById(1L)).thenReturn(Optional.empty());
    ConcertScheduleDto.UpdateRequest request =
        new ConcertScheduleDto.UpdateRequest(LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));

    assertThatThrownBy(() -> concertScheduleService.updateSchedule(1L, request))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.SCHEDULE_NOT_FOUND));
  }

  @Test
  @DisplayName("회차를 수정하면 날짜·시간이 갱신되고 잠정 상태였다면 확정으로 바뀐다")
  void updatesSchedule() {
    ConcertSchedule schedule = ConcertSchedule.createFromParsedSlot(
        concert(), 1, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));
    when(concertScheduleRepository.findWithConcertById(1L)).thenReturn(Optional.of(schedule));
    when(concertScheduleRepository.findByConcertIdAndPerformanceDate(
        any(), eq(LocalDate.of(2026, 9, 21))))
        .thenReturn(List.of(schedule));
    ConcertScheduleDto.UpdateRequest request =
        new ConcertScheduleDto.UpdateRequest(LocalDate.of(2026, 9, 21), LocalTime.of(20, 0));

    ConcertSchedule result = concertScheduleService.updateSchedule(1L, request);

    assertThat(result.getPerformanceDate()).isEqualTo(LocalDate.of(2026, 9, 21));
    assertThat(result.getPerformanceTime()).isEqualTo(LocalTime.of(20, 0));
    assertThat(result.getRound()).isEqualTo(1);
    assertThat(result.isProvisional()).isFalse();
    assertThat(result.getSource()).isEqualTo(ScheduleSource.MANUAL);
  }

  @Test
  @DisplayName("공연 기간 밖의 날짜로 회차를 수정하면 예외가 발생한다")
  void throwsWhenUpdatedPerformanceDateOutsideConcertPeriod() {
    ConcertSchedule schedule =
        ConcertSchedule.create(concert(), 1, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));
    when(concertScheduleRepository.findWithConcertById(1L)).thenReturn(Optional.of(schedule));
    ConcertScheduleDto.UpdateRequest request =
        new ConcertScheduleDto.UpdateRequest(LocalDate.of(2026, 8, 31), LocalTime.of(19, 30));

    assertThatThrownBy(() -> concertScheduleService.updateSchedule(1L, request))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.SCHEDULE_DATE_OUT_OF_RANGE));
  }

  @Test
  @DisplayName("회차를 삭제하면 하드 삭제 대신 소프트 삭제된다")
  void deletesSchedule() {
    ConcertSchedule schedule =
        ConcertSchedule.create(concert(), 1, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));
    when(concertScheduleRepository.findWithConcertById(1L)).thenReturn(Optional.of(schedule));

    concertScheduleService.deleteSchedule(1L);

    verify(concertScheduleRepository, never()).delete(any());
    assertThat(schedule.isDeleted()).isTrue();
    assertThat(schedule.getDeletedAt()).isEqualTo(clock.instant());
  }
}
