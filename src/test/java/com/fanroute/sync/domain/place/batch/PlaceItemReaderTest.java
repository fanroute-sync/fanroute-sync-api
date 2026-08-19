package com.fanroute.sync.domain.place.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import com.fanroute.sync.domain.place.client.TourApiClient;
import com.fanroute.sync.domain.place.config.TourApiProperties;
import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.PlaceCategory;

@ExtendWith(MockitoExtension.class)
class PlaceItemReaderTest {

  @Mock
  private TourApiClient tourApiClient;

  private TourApiProperties properties;

  @BeforeEach
  void setUp() {
    properties = new TourApiProperties("test-key", "26", 0);
  }

  @Test
  @DisplayName("숙박 카테고리는 searchStay2로 조회한다")
  void readsAccommodationViaSearchStay() {
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1)))
        .thenReturn(searchStayResponse(List.of(summary("1", "32")), 1));
    PlaceItemReader reader = openReader(PlaceCategory.ACCOMMODATION);

    assertThat(readAll(reader)).extracting(TourApiDto.PlaceSummary::contentId)
        .containsExactly("1");
    verify(tourApiClient, never()).searchAreaBasedList(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
        anyInt(), anyInt());
  }

  @Test
  @DisplayName("숙박 외 카테고리는 areaBasedList2를 해당 contentTypeId로 조회한다")
  void readsAttractionViaAreaBasedList() {
    when(tourApiClient.searchAreaBasedList(
        anyString(), anyString(), anyString(), anyString(), anyString(), eq("12"), anyString(),
        anyInt(), eq(1)))
        .thenReturn(areaBasedListResponse(List.of(summary("2", "12")), 1));
    PlaceItemReader reader = openReader(PlaceCategory.ATTRACTION);

    assertThat(readAll(reader)).extracting(TourApiDto.PlaceSummary::contentId)
        .containsExactly("2");
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
    PlaceItemReader reader = openReader(PlaceCategory.ACCOMMODATION);

    assertThat(readAll(reader)).extracting(TourApiDto.PlaceSummary::contentId)
        .containsExactly("1", "2");
    verify(tourApiClient, times(1)).searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1));
    verify(tourApiClient, times(1)).searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(2));
  }

  @Test
  @DisplayName("빈 응답을 받으면 더 조회하지 않고 종료한다")
  void stopsOnEmptyResponse() {
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1)))
        .thenReturn(searchStayResponse(List.of(), 0));
    PlaceItemReader reader = openReader(PlaceCategory.ACCOMMODATION);

    assertThat(reader.read()).isNull();
  }

  @Test
  @DisplayName("설정한 최대 건수까지만 반환한다")
  void limitsItemsByConfiguration() {
    properties = new TourApiProperties("test-key", "26", 1);
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1)))
        .thenReturn(searchStayResponse(
            List.of(summary("1", "32"), summary("2", "32")), 2));
    PlaceItemReader reader = openReader(PlaceCategory.ACCOMMODATION);

    assertThat(readAll(reader)).extracting(TourApiDto.PlaceSummary::contentId)
        .containsExactly("1");
  }

  private PlaceItemReader openReader(PlaceCategory category) {
    PlaceItemReader reader = new PlaceItemReader(tourApiClient, properties, category);
    reader.open(new ExecutionContext());
    return reader;
  }

  private List<TourApiDto.PlaceSummary> readAll(PlaceItemReader reader) {
    return Stream.generate(reader::read).takeWhile(item -> item != null).toList();
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
