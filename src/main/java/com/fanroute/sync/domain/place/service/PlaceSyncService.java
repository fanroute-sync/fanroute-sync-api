package com.fanroute.sync.domain.place.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fanroute.sync.domain.place.client.TourApiClient;
import com.fanroute.sync.domain.place.config.TourApiProperties;
import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.external.ExternalApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 페이지 누락을 막기 위해 페이지 실패 시 해당 카테고리 동기화를 중단합니다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceSyncService {

  private static final String MOBILE_OS = "ETC";
  private static final String MOBILE_APP = "FanRoute";
  private static final String RESPONSE_TYPE = "json";
  private static final String ARRANGE_BY_MODIFIED = "C"; // TourAPI 수정일순 코드
  private static final int PAGE_SIZE = 50;
  private static final int MAX_RETRY_COUNT = 3;

  private final TourApiClient tourApiClient;
  private final TourApiProperties tourApiProperties;
  private final PlaceUpsertService placeUpsertService;

  /** 카테고리별 실패를 격리해 나머지 동기화를 계속합니다. */
  public List<SyncOutcome> syncAll() {
    List<SyncOutcome> outcomes = new ArrayList<>();
    for (PlaceCategory category : PlaceCategory.values()) {
      try {
        outcomes.add(SyncOutcome.success(sync(category)));
      } catch (Exception exception) {
        log.error("TourAPI 장소 동기화 실패: category={}", category, exception);
        outcomes.add(SyncOutcome.failure(category, exception.getMessage()));
      }
    }
    return outcomes;
  }

  public SyncResult sync(PlaceCategory category) {
    int upsertedCount = 0;
    int pageNo = 1;
    while (true) {
      TourApiDto.PlaceListResponse response = fetchPageWithRetry(category, pageNo);
      List<TourApiDto.PlaceSummary> items = response.itemsOrEmpty();
      if (items.isEmpty()) {
        break;
      }
      placeUpsertService.upsertPage(category, items);
      upsertedCount += items.size();

      if ((long) pageNo * PAGE_SIZE >= response.totalCount()) {
        break;
      }
      pageNo++;
    }
    return new SyncResult(category, upsertedCount);
  }

  private TourApiDto.PlaceListResponse fetchPageWithRetry(PlaceCategory category, int pageNo) {
    for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
      try {
        return fetchPage(category, pageNo);
      } catch (ExternalApiException exception) {
        log.warn("TourAPI 목록 조회 실패(재시도 {}/{}): category={}, pageNo={}, message={}",
            attempt, MAX_RETRY_COUNT, category, pageNo, exception.getMessage());
      }
    }
    throw new BusinessException(PlaceErrorCode.TOUR_API_UNAVAILABLE);
  }

  private TourApiDto.PlaceListResponse fetchPage(PlaceCategory category, int pageNo) {
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

  public record SyncResult(PlaceCategory category, int upsertedCount) {

  }

  public record SyncOutcome(
      PlaceCategory category, boolean success, int upsertedCount, String failureMessage) {

    public static SyncOutcome success(SyncResult result) {
      return new SyncOutcome(result.category(), true, result.upsertedCount(), null);
    }

    public static SyncOutcome failure(PlaceCategory category, String failureMessage) {
      return new SyncOutcome(category, false, 0, failureMessage);
    }
  }
}
