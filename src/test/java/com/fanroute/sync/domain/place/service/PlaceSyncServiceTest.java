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
  @DisplayName("전체 결과가 한 페이지에 다 들어오면 한 번만 조회하고 종료한다")
  void stopsAfterSinglePage() {
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1)))
        .thenReturn(response(List.of(summary("1"), summary("2")), 2));

    PlaceSyncService.SyncResult result = service.sync(PlaceCategory.ACCOMMODATION);

    assertThat(result.category()).isEqualTo(PlaceCategory.ACCOMMODATION);
    assertThat(result.upsertedCount()).isEqualTo(2);
    verify(tourApiClient, times(1)).searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        anyInt());
  }

  @Test
  @DisplayName("totalCount가 한 페이지 크기를 넘으면 다음 페이지를 이어서 조회한다")
  void continuesToNextPageUntilTotalCountCovered() {
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1)))
        .thenReturn(response(List.of(summary("1")), 60));
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(2)))
        .thenReturn(response(List.of(summary("2")), 60));

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
        .thenReturn(response(List.of(summary("1")), 1));

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
  @DisplayName("아직 지원하지 않는 카테고리는 외부 API 호출 없이 예외를 던진다")
  void rejectsUnsupportedCategory() {
    assertThatThrownBy(() -> service.sync(PlaceCategory.ATTRACTION))
        .isInstanceOf(UnsupportedOperationException.class);
    verifyNoInteractions(tourApiClient);
  }

  private ExternalApiException serverError() {
    return ExternalApiException.responseError(
        ExternalApiErrorType.SERVER_ERROR, HttpStatus.BAD_GATEWAY, "server error");
  }

  private TourApiDto.SearchStayResponse response(
      List<TourApiDto.PlaceSummary> items, int totalCount) {
    TourApiDto.Items wrappedItems = new TourApiDto.Items(items);
    TourApiDto.Body body = new TourApiDto.Body(wrappedItems, items.size(), 1, totalCount);
    TourApiDto.Response response = new TourApiDto.Response(null, body);
    return new TourApiDto.SearchStayResponse(response);
  }

  private TourApiDto.PlaceSummary summary(String contentId) {
    return new TourApiDto.PlaceSummary(
        contentId, "32", "테스트 장소", "부산", null, null, 129.0, 35.1, null, null, null, null, "26",
        null, null, null, null, null, null);
  }
}
