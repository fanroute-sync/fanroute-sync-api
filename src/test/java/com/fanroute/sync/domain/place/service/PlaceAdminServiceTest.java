package com.fanroute.sync.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fanroute.sync.domain.place.client.TourApiClient;
import com.fanroute.sync.domain.concert.repository.VenueRepository;
import com.fanroute.sync.domain.place.config.TourApiProperties;
import com.fanroute.sync.domain.place.dto.PlaceDto;
import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.domain.place.repository.PlaceRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PlaceAdminServiceTest {

  @Mock
  private PlaceRepository placeRepository;
  @Mock
  private TourApiClient tourApiClient;
  @Mock
  private TourApiProperties tourApiProperties;
  @Mock
  private VenueRepository venueRepository;

  private PlaceAdminService placeAdminService;

  @BeforeEach
  void setUp() {
    placeAdminService = new PlaceAdminService(
        placeRepository, tourApiClient, tourApiProperties, venueRepository);
  }

  @Test
  @DisplayName("좌표 주변 후보를 TourAPI 조회순으로 조회한다")
  void searchesNearbyCandidates() {
    when(tourApiProperties.serviceKey()).thenReturn("test-key");
    when(tourApiClient.searchLocationBasedList(
        "test-key", "ETC", "FanRoute", "json", "Q", "39", 129.1364, 35.1691, 1000, 10, 1))
        .thenReturn(successResponse(List.of(placeSummary())));

    List<TourApiDto.PlaceSummary> result = placeAdminService.searchNearbyCandidates(
        PlaceCategory.RESTAURANT, 35.1691, 129.1364, 1000);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).contentId()).isEqualTo("2868824");
  }

  @Test
  @DisplayName("TourAPI 응답이 실패 코드면 예외가 발생한다")
  void throwsWhenTourApiResponseFails() {
    when(tourApiProperties.serviceKey()).thenReturn("test-key");
    when(tourApiClient.searchLocationBasedList(
        any(), any(), any(), any(), any(), any(), anyDouble(), anyDouble(), anyInt(), anyInt(),
        anyInt()))
        .thenReturn(new TourApiDto.LocationBasedListResponse(
            new TourApiDto.Response(new TourApiDto.Header("9999", "오류"), null)));

    assertThatThrownBy(() -> placeAdminService.searchNearbyCandidates(
        PlaceCategory.RESTAURANT, 35.1691, 129.1364, 1000))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(PlaceErrorCode.TOUR_API_RESPONSE_INVALID));
  }

  @Test
  @DisplayName("contentId로 이미 동기화된 장소를 찾는다")
  void findsExistingPlaceByContentId() {
    Place place = Place.create(
        "126508", PlaceCategory.ACCOMMODATION, "32", "테스트 호텔", "부산", null, null, 35.1, 129.0,
        null, null, null, null, "26", null, null, null, null, null, null, Instant.now());
    when(placeRepository.findByContentId("126508")).thenReturn(Optional.of(place));

    assertThat(placeAdminService.findExistingByContentId("126508")).isSameAs(place);
  }

  @Test
  @DisplayName("아직 동기화되지 않은 contentId는 null을 반환한다")
  void returnsNullWhenContentIdNotSynced() {
    when(placeRepository.findByContentId("999")).thenReturn(Optional.empty());

    assertThat(placeAdminService.findExistingByContentId("999")).isNull();
  }

  @Test
  @DisplayName("검수한 후보를 contentId 기준으로 Place에 등록한다")
  void importsCandidateAsPlace() {
    when(placeRepository.findByContentId("2868824")).thenReturn(Optional.empty());
    when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));

    List<Place> result = placeAdminService.importCandidates(PlaceCategory.RESTAURANT, List.of(
        new PlaceDto.CandidateImportRequest(
            "2868824", "비스포레", "부산광역시 수영구", null, null,
            35.1637, 129.1287, null, null, null, null)));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContentId()).isEqualTo("2868824");
    assertThat(result.get(0).getCategory()).isEqualTo(PlaceCategory.RESTAURANT);
  }

  private TourApiDto.LocationBasedListResponse successResponse(
      List<TourApiDto.PlaceSummary> items) {
    return new TourApiDto.LocationBasedListResponse(new TourApiDto.Response(
        new TourApiDto.Header(TourApiDto.SUCCESS_RESULT_CODE, "OK"),
        new TourApiDto.Body(new TourApiDto.Items(items), items.size(), 1, items.size())));
  }

  private TourApiDto.PlaceSummary placeSummary() {
    return new TourApiDto.PlaceSummary(
        "2868824", "39", "비스포레", "부산광역시 수영구", null, null, 129.1287, 35.1637, null, null,
        null, null, "26", null, null, null, null, null, null, 910.87);
  }
}
