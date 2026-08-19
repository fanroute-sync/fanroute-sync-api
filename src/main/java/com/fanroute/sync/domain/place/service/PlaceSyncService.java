package com.fanroute.sync.domain.place.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
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

/** 페이지 누락을 막기 위해 한 페이지가 실패하면 해당 카테고리 동기화 전체를 중단합니다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceSyncService {

  private static final String MOBILE_OS = "ETC";
  private static final String MOBILE_APP = "FanRoute";
  private static final String RESPONSE_TYPE = "json";
  private static final String ARRANGE_BY_MODIFIED = "C";
  private static final int PAGE_SIZE = 50;
  private static final int MAX_RETRY_COUNT = 3;

  private final TourApiClient tourApiClient;
  private final TourApiProperties tourApiProperties;
  private final PlaceUpsertService placeUpsertService;

  @Scheduled(cron = "${tour-api.sync-cron:0 0 5 * * *}")
  public void syncAccommodationsOnSchedule() {
    SyncResult result = sync(PlaceCategory.ACCOMMODATION);
    log.info("TourAPI 장소 정기 동기화 완료: category={}, upsertedCount={}",
        result.category(), result.upsertedCount());
  }

  public SyncResult sync(PlaceCategory category) {
    validateSupported(category);

    int upsertedCount = 0;
    int pageNo = 1;
    while (true) {
      TourApiDto.SearchStayResponse response = fetchPageWithRetry(pageNo);
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

  private void validateSupported(PlaceCategory category) {
    if (category != PlaceCategory.ACCOMMODATION) {
      throw new UnsupportedOperationException(
          "아직 지원하지 않는 장소 카테고리입니다: " + category);
    }
  }

  private TourApiDto.SearchStayResponse fetchPageWithRetry(int pageNo) {
    for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
      try {
        return tourApiClient.searchStay(
            tourApiProperties.serviceKey(), MOBILE_OS, MOBILE_APP, RESPONSE_TYPE,
            ARRANGE_BY_MODIFIED, tourApiProperties.legalDongRegionCode(), PAGE_SIZE, pageNo);
      } catch (ExternalApiException exception) {
        log.warn("TourAPI 목록 조회 실패(재시도 {}/{}): pageNo={}, message={}",
            attempt, MAX_RETRY_COUNT, pageNo, exception.getMessage());
      }
    }
    throw new BusinessException(PlaceErrorCode.TOUR_API_UNAVAILABLE);
  }

  public record SyncResult(PlaceCategory category, int upsertedCount) {

  }
}
