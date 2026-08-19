package com.fanroute.sync.domain.place.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.domain.place.repository.PlaceRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 프록시 기반 트랜잭션으로 페이지 단위 원자성을 보장하기 위해 별도 Bean으로 분리합니다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceUpsertService {

  private final PlaceRepository placeRepository;
  private final Clock clock;

  @Transactional
  public void upsertPage(PlaceCategory category, List<TourApiDto.PlaceSummary> items) {
    for (TourApiDto.PlaceSummary item : items) {
      upsertPlace(category, item);
    }
  }

  private void upsertPlace(PlaceCategory category, TourApiDto.PlaceSummary item) {
    if (item.contentId() == null || item.contentId().isBlank()) {
      throw new BusinessException(PlaceErrorCode.TOUR_API_RESPONSE_INVALID);
    }
    // 잘못 섞인 항목만 제외해 정상 항목의 페이지 저장은 유지합니다.
    if (!matchesCategory(category, item.contentTypeId())) {
      log.warn(
          "TourAPI 응답의 contentTypeId가 요청 카테고리와 다릅니다: category={}, contentId={}, "
              + "contentTypeId={}",
          category, item.contentId(), item.contentTypeId());
      return;
    }
    Instant syncedAt = clock.instant();

    placeRepository.findByContentId(item.contentId())
        .ifPresentOrElse(
            place -> place.updateFromSync(
                item.contentTypeId(), item.name(), item.address(), item.detailAddress(),
                item.zipCode(), item.latitude(), item.longitude(), item.telephone(),
                item.imageUrl(), item.thumbnailUrl(), item.copyrightType(),
                item.legalDongRegionCode(), item.legalDongSignguCode(),
                item.classificationLevel1(), item.classificationLevel2(),
                item.classificationLevel3(), item.sourceCreatedAt(), item.sourceModifiedAt(),
                syncedAt),
            () -> placeRepository.save(Place.create(
                item.contentId(), category, item.contentTypeId(), item.name(), item.address(),
                item.detailAddress(), item.zipCode(), item.latitude(), item.longitude(),
                item.telephone(), item.imageUrl(), item.thumbnailUrl(), item.copyrightType(),
                item.legalDongRegionCode(), item.legalDongSignguCode(),
                item.classificationLevel1(), item.classificationLevel2(),
                item.classificationLevel3(), item.sourceCreatedAt(), item.sourceModifiedAt(),
                syncedAt)));
  }

  private boolean matchesCategory(PlaceCategory category, String contentTypeId) {
    try {
      return PlaceCategory.fromContentTypeId(contentTypeId) == category;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }
}
