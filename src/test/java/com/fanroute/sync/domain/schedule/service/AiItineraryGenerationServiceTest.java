package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.AiItineraryGenerationRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryDayRepository;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class AiItineraryGenerationServiceTest {

  @Mock
  private AiItineraryGenerationRepository generationRepository;
  @Mock
  private ItineraryDayRepository itineraryDayRepository;

  @Test
  @DisplayName("내 날짜별 일정에 PENDING AI 생성 작업을 만든다")
  void requestsGeneration() {
    ItineraryDay itineraryDay = itineraryDay();
    when(itineraryDayRepository.findById(1L)).thenReturn(Optional.of(itineraryDay));
    when(generationRepository.save(any(AiItineraryGeneration.class))).thenAnswer(invocation -> {
      AiItineraryGeneration generation = invocation.getArgument(0);
      ReflectionTestUtils.setField(generation, "id", 10L);
      return generation;
    });

    AiItineraryGenerationDto.CreateResponse response = service().request(
        UserFixture.activeUserWithId(1L), 1L);

    assertThat(response.generationId()).isEqualTo(10L);
    assertThat(response.status()).isEqualTo(AiItineraryGenerationStatus.PENDING);
  }

  @Test
  @DisplayName("다른 사용자의 AI 생성 작업 상태는 조회할 수 없다")
  void rejectsStatusLookupForOtherUser() {
    when(generationRepository.findByIdAndItineraryDayTripPlanUserId(10L, 2L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getStatus(UserFixture.activeUserWithId(2L), 10L))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.AI_ITINERARY_GENERATION_NOT_FOUND);
  }

  private AiItineraryGenerationService service() {
    return new AiItineraryGenerationService(generationRepository, itineraryDayRepository);
  }

  private ItineraryDay itineraryDay() {
    TripPlan tripPlan = TripPlan.create(UserFixture.activeUserWithId(1L), null,
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        null, List.of(), List.of());
    ItineraryDay itineraryDay = ItineraryDay.create(tripPlan, LocalDate.of(2026, 9, 1), false);
    ReflectionTestUtils.setField(itineraryDay, "id", 1L);
    return itineraryDay;
  }
}
