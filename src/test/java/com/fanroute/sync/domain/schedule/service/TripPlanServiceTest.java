package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.service.ConcertScheduleService;
import com.fanroute.sync.domain.concert.service.ConcertService;
import com.fanroute.sync.domain.schedule.dto.TripPlanDto;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.TravelTimeSlot;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.ItineraryDayRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryItemRepository;
import com.fanroute.sync.domain.schedule.repository.TripPlanRepository;
import com.fanroute.sync.domain.schedule.repository.AccommodationRepository;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class TripPlanServiceTest {

  @Mock
  private TripPlanRepository tripPlanRepository;
  @Mock
  private ItineraryDayRepository itineraryDayRepository;
  @Mock
  private ItineraryItemRepository itineraryItemRepository;
  @Mock
  private AccommodationRepository accommodationRepository;
  @Mock
  private ConcertService concertService;
  @Mock
  private ConcertScheduleService concertScheduleService;

  @Test
  @DisplayName("프론트 시간대 값을 KST 기준 Instant로 변환해 날짜별 일정을 생성한다")
  void createsTripPlanAndItineraryDays() {
    TripPlanService service = service();
    when(tripPlanRepository.save(any(TripPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(itineraryDayRepository.saveAll(any())).thenAnswer(invocation -> {
      List<ItineraryDay> days = invocation.getArgument(0);
      return days;
    });

    TripPlanDto.CreateResponse response = service.create(UserFixture.activeUser(),
        new TripPlanDto.CreateRequest(LocalDate.of(2026, 9, 1), TravelTimeSlot.MORNING,
            LocalDate.of(2026, 9, 3), TravelTimeSlot.EVENING, null, null));

    assertThat(response.arrivalAt()).isEqualTo(Instant.parse("2026-09-01T00:00:00Z"));
    assertThat(response.departureAt()).isEqualTo(Instant.parse("2026-09-03T09:00:00Z"));
    assertThat(response.itineraryDays()).extracting(TripPlanDto.ItineraryDayResponse::date)
        .containsExactly(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2),
            LocalDate.of(2026, 9, 3));
  }

  @Test
  @DisplayName("도착 시각이 출발 시각보다 늦으면 여행 계획을 생성할 수 없다")
  void rejectsInvalidTripPeriod() {
    TripPlanService service = service();

    assertThatThrownBy(() -> service.create(UserFixture.activeUser(),
        new TripPlanDto.CreateRequest(LocalDate.of(2026, 9, 3), TravelTimeSlot.EVENING,
            LocalDate.of(2026, 9, 3), TravelTimeSlot.MORNING, null, null)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.INVALID_TRIP_PERIOD);
  }

  @Test
  @DisplayName("공연에 속한 회차를 지정하면 여행 계획에 연결된다")
  void assignsConcertScheduleWhenItBelongsToConcert() {
    TripPlanService service = service();
    Concert concert = concert();
    ReflectionTestUtils.setField(concert, "id", 1L);
    when(concertService.getConcert(1L)).thenReturn(concert);
    ConcertSchedule schedule =
        ConcertSchedule.create(concert, 1, LocalDate.of(2026, 9, 2), LocalTime.of(19, 0));
    when(concertScheduleService.getSchedule(5L)).thenReturn(schedule);
    when(tripPlanRepository.save(any(TripPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(itineraryDayRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TripPlanDto.CreateResponse response = service.create(UserFixture.activeUser(),
        new TripPlanDto.CreateRequest(LocalDate.of(2026, 9, 1), TravelTimeSlot.MORNING,
            LocalDate.of(2026, 9, 3), TravelTimeSlot.EVENING, 1L, 5L));

    assertThat(response.concertScheduleId()).isEqualTo(5L);
  }

  @Test
  @DisplayName("다른 공연의 회차를 지정하면 여행 계획 생성을 거부한다")
  void rejectsConcertScheduleFromDifferentConcert() {
    TripPlanService service = service();
    Concert concert = concert();
    ReflectionTestUtils.setField(concert, "id", 1L);
    when(concertService.getConcert(1L)).thenReturn(concert);
    Concert otherConcert = concert();
    ReflectionTestUtils.setField(otherConcert, "id", 2L);
    ConcertSchedule otherSchedule =
        ConcertSchedule.create(otherConcert, 1, LocalDate.of(2026, 9, 2), LocalTime.of(19, 0));
    when(concertScheduleService.getSchedule(5L)).thenReturn(otherSchedule);

    assertThatThrownBy(() -> service.create(UserFixture.activeUser(),
        new TripPlanDto.CreateRequest(LocalDate.of(2026, 9, 1), TravelTimeSlot.MORNING,
            LocalDate.of(2026, 9, 3), TravelTimeSlot.EVENING, 1L, 5L)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.CONCERT_SCHEDULE_MISMATCH);
  }

  private Concert concert() {
    Venue venue = Venue.create("MT10TEST", "테스트 공연장", "부산광역시", 35.1, 129.1);
    return Concert.create("MT20TEST", venue, "테스트 콘서트", Genre.POPULAR_MUSIC,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), null,
        Instant.parse("2026-08-01T00:00:00Z"));
  }

  @Test
  @DisplayName("내 여행 계획만 목록으로 조회한다")
  void getsMyTripPlans() {
    TripPlanService service = service();
    TripPlan tripPlan = TripPlan.create(UserFixture.activeUser(), null,
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        null, List.of(), List.of());
    when(tripPlanRepository.findByUserIdOrderByCreatedAtDesc(null)).thenReturn(List.of(tripPlan));

    assertThat(service.getMyTripPlans(UserFixture.activeUser())).hasSize(1);
  }

  @Test
  @DisplayName("여행 계획을 삭제할 때 종속 데이터를 먼저 삭제한다")
  void deletesTripPlanWithChildren() {
    TripPlanService service = service();
    TripPlan tripPlan = TripPlan.create(UserFixture.activeUser(), null,
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        null, List.of(), List.of());
    when(tripPlanRepository.findByIdAndUserId(1L, null)).thenReturn(Optional.of(tripPlan));

    service.deleteTripPlan(UserFixture.activeUser(), 1L);

    verify(itineraryItemRepository).deleteByItineraryDayTripPlanId(1L);
    verify(itineraryDayRepository).deleteByTripPlanId(1L);
    verify(accommodationRepository).deleteByTripPlanId(1L);
    verify(tripPlanRepository).deleteById(1L);
  }

  private TripPlanService service() {
    return new TripPlanService(tripPlanRepository, itineraryDayRepository, itineraryItemRepository,
        accommodationRepository, concertService, concertScheduleService);
  }
}
