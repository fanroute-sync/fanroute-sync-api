package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fanroute.sync.domain.schedule.dto.TripPlanDto;
import com.fanroute.sync.domain.schedule.entity.TravelIntensityType;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.repository.TripPlanRepository;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class TravelStyleServiceTest {

  @Mock
  private TripPlanRepository tripPlanRepository;

  @Test
  @DisplayName("여행 강도, 복수 동행, 여행 MBTI를 저장한다")
  void updatesTravelStyle() {
    TripPlan tripPlan = TripPlan.create(UserFixture.activeUser(), null,
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"), null,
        List.of(), List.of("맛집"));
    when(tripPlanRepository.findByIdAndUserId(1L, null)).thenReturn(Optional.of(tripPlan));

    TripPlanDto.TravelStyleResponse response = service().update(UserFixture.activeUser(), 1L,
        new TripPlanDto.UpdateTravelStyleRequest(TravelIntensityType.TIGHT,
            List.of("친구", "아이"), "맛집탐방형"));

    assertThat(response.travelIntensity()).isEqualTo(TravelIntensityType.TIGHT);
    assertThat(response.companions()).containsExactly("친구", "아이");
    assertThat(response.travelMbti()).isEqualTo("맛집탐방형");
    assertThat(tripPlan.getPreferences()).containsExactly("맛집");
  }

  private TravelStyleService service() {
    return new TravelStyleService(tripPlanRepository);
  }
}
