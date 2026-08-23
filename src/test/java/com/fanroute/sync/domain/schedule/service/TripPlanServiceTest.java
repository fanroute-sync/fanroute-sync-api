package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

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

  @Test
  @DisplayName("프론트 시간대 값을 KST 기준 Instant로 변환해 날짜별 일정을 생성한다")
  void createsTripPlanAndItineraryDays() {
    TripPlanService service = new TripPlanService(
        tripPlanRepository, itineraryDayRepository, itineraryItemRepository, accommodationRepository,
        concertService);
    when(tripPlanRepository.save(any(TripPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(itineraryDayRepository.saveAll(any())).thenAnswer(invocation -> {
      List<ItineraryDay> days = invocation.getArgument(0);
      return days;
    });

    TripPlanDto.CreateResponse response = service.create(UserFixture.activeUser(),
        new TripPlanDto.CreateRequest(LocalDate.of(2026, 9, 1), TravelTimeSlot.MORNING,
            LocalDate.of(2026, 9, 3), TravelTimeSlot.EVENING, null));

    assertThat(response.arrivalAt()).isEqualTo(Instant.parse("2026-09-01T00:00:00Z"));
    assertThat(response.departureAt()).isEqualTo(Instant.parse("2026-09-03T09:00:00Z"));
    assertThat(response.itineraryDays()).extracting(TripPlanDto.ItineraryDayResponse::date)
        .containsExactly(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2),
            LocalDate.of(2026, 9, 3));
  }

  @Test
  @DisplayName("도착 시각이 출발 시각보다 늦으면 여행 계획을 생성할 수 없다")
  void rejectsInvalidTripPeriod() {
    TripPlanService service = new TripPlanService(
        tripPlanRepository, itineraryDayRepository, itineraryItemRepository, accommodationRepository,
        concertService);

    assertThatThrownBy(() -> service.create(UserFixture.activeUser(),
        new TripPlanDto.CreateRequest(LocalDate.of(2026, 9, 3), TravelTimeSlot.EVENING,
            LocalDate.of(2026, 9, 3), TravelTimeSlot.MORNING, null)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.INVALID_TRIP_PERIOD);
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
    when(tripPlanRepository.findByIdAndUserId(1L, null)).thenReturn(java.util.Optional.of(tripPlan));

    service.deleteTripPlan(UserFixture.activeUser(), 1L);

    verify(itineraryItemRepository).deleteByItineraryDayTripPlanId(1L);
    verify(itineraryDayRepository).deleteByTripPlanId(1L);
    verify(accommodationRepository).deleteByTripPlanId(1L);
    verify(tripPlanRepository).deleteById(1L);
  }

  private TripPlanService service() {
    return new TripPlanService(tripPlanRepository, itineraryDayRepository, itineraryItemRepository,
        accommodationRepository, concertService);
  }
}
