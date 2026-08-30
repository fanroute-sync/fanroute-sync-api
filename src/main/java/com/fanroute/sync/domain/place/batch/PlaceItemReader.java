package com.fanroute.sync.domain.place.batch;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import com.fanroute.sync.domain.place.client.TourApiClient;
import com.fanroute.sync.domain.place.config.TourApiProperties;
import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/** 카테고리에 맞는 TourAPI 목록을 페이지 단위로 조회합니다. */
@RequiredArgsConstructor
public class PlaceItemReader implements ItemStreamReader<TourApiDto.PlaceSummary> {

  private static final String MOBILE_OS = "ETC";
  private static final String MOBILE_APP = "FanRoute";
  private static final String RESPONSE_TYPE = "json";
  private static final String ARRANGE_BY_MODIFIED = "C"; // TourAPI 수정일순 코드
  private static final int PAGE_SIZE = 50;

  private final TourApiClient tourApiClient;
  private final TourApiProperties tourApiProperties;
  private final PlaceCategory category;

  private Queue<TourApiDto.PlaceSummary> buffer;
  private int pageNo;
  private int totalCount;
  private int returnedCount;
  private boolean exhausted;

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    this.buffer = new ArrayDeque<>();
    this.pageNo = 1;
    this.totalCount = Integer.MAX_VALUE;
    this.returnedCount = 0;
    this.exhausted = false;
  }

  @Override
  public TourApiDto.PlaceSummary read() {
    if (reachedSyncLimit()) {
      return null;
    }
    if (buffer.isEmpty() && !exhausted) {
      fetchNextPage();
    }
    TourApiDto.PlaceSummary item = buffer.poll();
    if (item != null) {
      returnedCount++;
    }
    return item;
  }

  /** 조회 성공 후 페이지를 증가시켜 재시도 시 같은 페이지를 다시 요청합니다. */
  private void fetchNextPage() {
    TourApiDto.PlaceListResponse response = fetchPage(pageNo);
    // HTTP 200이어도 오류 응답일 수 있어 resultCode를 확인합니다.
    if (!response.isSuccess()) {
      throw new BusinessException(PlaceErrorCode.TOUR_API_RESPONSE_INVALID);
    }
    totalCount = response.totalCount();
    List<TourApiDto.PlaceSummary> items = response.itemsOrEmpty();
    if (items.isEmpty()) {
      exhausted = true;
      return;
    }
    buffer.addAll(items);
    if ((long) pageNo * PAGE_SIZE >= totalCount) {
      exhausted = true;
    }
    pageNo++;
  }

  private TourApiDto.PlaceListResponse fetchPage(int pageNo) {
    if (category == PlaceCategory.ACCOMMODATION) {
      return tourApiClient.searchStay(
          tourApiProperties.serviceKey(), MOBILE_OS, MOBILE_APP, RESPONSE_TYPE,
          ARRANGE_BY_MODIFIED, tourApiProperties.legalDongRegionCode(), PAGE_SIZE, pageNo);
    }
    return tourApiClient.searchAreaBasedList(
        tourApiProperties.serviceKey(), MOBILE_OS, MOBILE_APP, RESPONSE_TYPE,
        ARRANGE_BY_MODIFIED, category.tourApiContentTypeId(),
        tourApiProperties.legalDongRegionCode(), PAGE_SIZE, pageNo);
  }

  private boolean reachedSyncLimit() {
    return tourApiProperties.syncMaxItems() > 0
        && returnedCount >= tourApiProperties.syncMaxItems();
  }
}
