package com.fanroute.sync.domain.place.batch;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 식별자가 없는 응답은 실패시키고 요청 카테고리와 다른 항목은 필터링합니다. */
@RequiredArgsConstructor
@Slf4j
public class PlaceItemProcessor
    implements ItemProcessor<TourApiDto.PlaceSummary, TourApiDto.PlaceSummary> {

  private final PlaceCategory category;

  @Override
  public TourApiDto.PlaceSummary process(TourApiDto.PlaceSummary item) {
    if (item.contentId() == null || item.contentId().isBlank()) {
      throw new BusinessException(PlaceErrorCode.TOUR_API_RESPONSE_INVALID);
    }
    if (!matchesCategory(item.contentTypeId())) {
      log.warn(
          "TourAPI 응답의 contentTypeId가 요청 카테고리와 다릅니다: category={}, contentId={}, "
              + "contentTypeId={}",
          category, item.contentId(), item.contentTypeId());
      return null;
    }
    return item;
  }

  private boolean matchesCategory(String contentTypeId) {
    try {
      return PlaceCategory.fromContentTypeId(contentTypeId) == category;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }
}
