package com.fanroute.sync.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fanroute.sync.domain.place.client.TourApiClient;
import com.fanroute.sync.domain.place.config.TourApiProperties;
import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.external.ExternalApiErrorType;
import com.fanroute.sync.global.external.ExternalApiException;

@ExtendWith(MockitoExtension.class)
class PlaceSyncServiceTest {

  @Mock
  private TourApiClient tourApiClient;
  @Mock
  private PlaceUpsertService placeUpsertService;

  private PlaceSyncService service;

  @BeforeEach
  void setUp() {
    TourApiProperties properties = new TourApiProperties("test-key", "26");
    service = new PlaceSyncService(tourApiClient, properties, placeUpsertService);
  }

  @Test
  @DisplayName("숙박은 searchStay2로 조회한다")
  void syncsAccommodationViaSearchStay() {
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1)))
        .thenReturn(searchStayResponse(List.of(summary("1", "32")), 1));

    PlaceSyncService.SyncResult result = service.sync(PlaceCategory.ACCOMMODATION);

    assertThat(result.upsertedCount()).isEqualTo(1);
    verify(tourApiClient, times(1)).searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        anyInt());
  }

  @Test
  @DisplayName("관광지는 areaBasedList2를 contentTypeId=12로 호출한다")
  void syncsAttractionViaAreaBasedList() {
    when(tourApiClient.searchAreaBasedList(
        anyString(), anyString(), anyString(), anyString(), anyString(), eq("12"), anyString(),
        anyInt(), eq(1)))
        .thenReturn(areaBasedListResponse(List.of(summary("1", "12")), 1));

    PlaceSyncService.SyncResult result = service.sync(PlaceCategory.ATTRACTION);

    assertThat(result.category()).isEqualTo(PlaceCategory.ATTRACTION);
    assertThat(result.upsertedCount()).isEqualTo(1);
    verify(tourApiClient, times(1)).searchAreaBasedList(
        anyString(), anyString(), anyString(), anyString(), anyString(), eq("12"), anyString(),
        anyInt(), anyInt());
  }

  @Test
  @DisplayName("음식점은 areaBasedList2를 contentTypeId=39로 호출한다")
  void syncsRestaurantViaAreaBasedList() {
    when(tourApiClient.searchAreaBasedList(
        anyString(), anyString(), anyString(), anyString(), anyString(), eq("39"), anyString(),
        anyInt(), eq(1)))
        .thenReturn(areaBasedListResponse(List.of(summary("1", "39")), 1));

    PlaceSyncService.SyncResult result = service.sync(PlaceCategory.RESTAURANT);

    assertThat(result.category()).isEqualTo(PlaceCategory.RESTAURANT);
    assertThat(result.upsertedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("totalCount가 한 페이지 크기를 넘으면 다음 페이지를 이어서 조회한다")
  void continuesToNextPageUntilTotalCountCovered() {
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1)))
        .thenReturn(searchStayResponse(List.of(summary("1", "32")), 60));
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(2)))
        .thenReturn(searchStayResponse(List.of(summary("2", "32")), 60));

    PlaceSyncService.SyncResult result = service.sync(PlaceCategory.ACCOMMODATION);

    assertThat(result.upsertedCount()).isEqualTo(2);
    verify(tourApiClient, times(1)).searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1));
    verify(tourApiClient, times(1)).searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(2));
  }

  @Test
  @DisplayName("일시적으로 실패해도 재시도 안에 성공하면 정상 처리된다")
  void succeedsAfterTransientFailure() {
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1)))
        .thenThrow(serverError())
        .thenReturn(searchStayResponse(List.of(summary("1", "32")), 1));

    PlaceSyncService.SyncResult result = service.sync(PlaceCategory.ACCOMMODATION);

    assertThat(result.upsertedCount()).isEqualTo(1);
    verify(tourApiClient, times(2)).searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1));
  }

  @Test
  @DisplayName("페이지가 재시도 후에도 실패하면 카테고리 동기화 전체를 실패로 처리한다")
  void failsWholeCategoryWhenPageFailsAfterRetries() {
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1)))
        .thenThrow(serverError());

    assertThatThrownBy(() -> service.sync(PlaceCategory.ACCOMMODATION))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(PlaceErrorCode.TOUR_API_UNAVAILABLE));
    verify(tourApiClient, times(3)).searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1));
    verifyNoInteractions(placeUpsertService);
  }

  @Test
  @DisplayName("syncAll은 한 카테고리가 실패해도 나머지 카테고리는 계속 진행한다")
  void syncAllIsolatesFailurePerCategory() {
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1)))
        .thenReturn(searchStayResponse(List.of(summary("1", "32")), 1));
    when(tourApiClient.searchAreaBasedList(
        anyString(), anyString(), anyString(), anyString(), anyString(), eq("12"), anyString(),
        anyInt(), eq(1)))
        .thenThrow(serverError());
    when(tourApiClient.searchAreaBasedList(
        anyString(), anyString(), anyString(), anyString(), anyString(), eq("39"), anyString(),
        anyInt(), eq(1)))
        .thenReturn(areaBasedListResponse(List.of(summary("2", "39")), 1));

    List<PlaceSyncService.SyncOutcome> outcomes = service.syncAll();

    assertThat(outcomes).hasSize(3);
    assertThat(outcomes.get(0).category()).isEqualTo(PlaceCategory.ACCOMMODATION);
    assertThat(outcomes.get(0).success()).isTrue();
    assertThat(outcomes.get(0).upsertedCount()).isEqualTo(1);
    assertThat(outcomes.get(1).category()).isEqualTo(PlaceCategory.ATTRACTION);
    assertThat(outcomes.get(1).success()).isFalse();
    assertThat(outcomes.get(2).category()).isEqualTo(PlaceCategory.RESTAURANT);
    assertThat(outcomes.get(2).success()).isTrue();
    assertThat(outcomes.get(2).upsertedCount()).isEqualTo(1);
  }

  private ExternalApiException serverError() {
    return ExternalApiException.responseError(
        ExternalApiErrorType.SERVER_ERROR, HttpStatus.BAD_GATEWAY, "server error");
  }

  private TourApiDto.SearchStayResponse searchStayResponse(
      List<TourApiDto.PlaceSummary> items, int totalCount) {
    return new TourApiDto.SearchStayResponse(responseBody(items, totalCount));
  }

  private TourApiDto.AreaBasedListResponse areaBasedListResponse(
      List<TourApiDto.PlaceSummary> items, int totalCount) {
    return new TourApiDto.AreaBasedListResponse(responseBody(items, totalCount));
  }

  private TourApiDto.Response responseBody(List<TourApiDto.PlaceSummary> items, int totalCount) {
    TourApiDto.Items wrappedItems = new TourApiDto.Items(items);
    TourApiDto.Body body = new TourApiDto.Body(wrappedItems, items.size(), 1, totalCount);
    return new TourApiDto.Response(null, body);
  }

  private TourApiDto.PlaceSummary summary(String contentId, String contentTypeId) {
    return new TourApiDto.PlaceSummary(
        contentId, contentTypeId, "테스트 장소", "부산", null, null, 129.0, 35.1, null, null, null,
        null, "26", null, null, null, null, null, null);
  }
}
