package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

import com.fanroute.sync.domain.place.repository.PlaceRepository;
import com.fanroute.sync.domain.schedule.client.KakaoLocalClient;
import com.fanroute.sync.domain.schedule.dto.TripPlanDto;
import com.fanroute.sync.domain.schedule.entity.Accommodation;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.AccommodationRepository;
import com.fanroute.sync.domain.schedule.repository.TripPlanRepository;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class AccommodationServiceTest {

  @Mock
  private TripPlanRepository tripPlanRepository;
  @Mock
  private AccommodationRepository accommodationRepository;
  @Mock
  private PlaceRepository placeRepository;
  @Mock
  private KakaoLocalClient kakaoLocalClient;

  @Test
  @DisplayName("직접 입력 숙소는 카카오 좌표를 저장하고 기존 목록을 교체한다")
  void replacesAccommodationsWithGeocodedAddress() {
    TripPlan tripPlan = tripPlan();
    when(tripPlanRepository.findByIdAndUserId(1L, null)).thenReturn(Optional.of(tripPlan));
    when(kakaoLocalClient.findCoordinates("부산 해운대구 해운대해변로 296"))
        .thenReturn(Optional.of(new KakaoLocalClient.Coordinates(35.1587, 129.1604)));
    when(accommodationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    List<TripPlanDto.AccommodationResponse> result = service().replace(UserFixture.activeUser(), 1L,
        new TripPlanDto.ReplaceAccommodationsRequest(List.of(
            new TripPlanDto.CreateAccommodationRequest(null, "부산 해운대구 해운대해변로 296",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3)))));

    verify(accommodationRepository).deleteByTripPlanId(1L);
    assertThat(result).singleElement().satisfies(accommodation -> {
      assertThat(accommodation.latitude()).isEqualTo(35.1587);
      assertThat(accommodation.longitude()).isEqualTo(129.1604);
    });
  }

  @Test
  @DisplayName("겹치는 숙소 기간은 기존 숙소를 지우기 전에 거절한다")
  void rejectsOverlappingAccommodations() {
    TripPlan tripPlan = tripPlan();
    when(tripPlanRepository.findByIdAndUserId(1L, null)).thenReturn(Optional.of(tripPlan));
    TripPlanDto.ReplaceAccommodationsRequest request = new TripPlanDto.ReplaceAccommodationsRequest(
        List.of(
            new TripPlanDto.CreateAccommodationRequest(null, "숙소 A", LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 3)),
            new TripPlanDto.CreateAccommodationRequest(null, "숙소 B", LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 3))));

    assertThatThrownBy(() -> service().replace(UserFixture.activeUser(), 1L, request))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.INVALID_ACCOMMODATION);
  }

  private AccommodationService service() {
    return new AccommodationService(tripPlanRepository, accommodationRepository, placeRepository,
        kakaoLocalClient);
  }

  private TripPlan tripPlan() {
    return TripPlan.create(UserFixture.activeUser(), null,
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"), null,
        List.of(), List.of());
  }
}
