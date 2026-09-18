package com.fanroute.sync.domain.place.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.place.client.TourApiClient;
import com.fanroute.sync.domain.place.config.TourApiProperties;
import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.domain.place.repository.PlaceRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/** 관리자 전용 장소 큐레이션 보조 기능. 일반 사용자 경로의 {@link PlaceService}와 분리해 관리합니다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceAdminService {

  private static final String MOBILE_OS = "ETC";
  private static final String MOBILE_APP = "FanRoute";
  private static final String RESPONSE_TYPE = "json";
  private static final String ARRANGE_BY_VIEW_COUNT = "Q"; // TourAPI 조회순(인기순 근사값) 코드
  private static final int MAX_CANDIDATES = 10;

  private final PlaceRepository placeRepository;
  private final TourApiClient tourApiClient;
  private final TourApiProperties tourApiProperties;

  /** 좌표 기준 반경 안에서 TourAPI 조회순으로 후보를 찾습니다. 결과를 저장하지 않는 실시간 조회입니다. */
  public List<TourApiDto.PlaceSummary> searchNearbyCandidates(
      PlaceCategory category, double latitude, double longitude, int radiusMeters) {
    TourApiDto.LocationBasedListResponse response = tourApiClient.searchLocationBasedList(
        tourApiProperties.serviceKey(), MOBILE_OS, MOBILE_APP, RESPONSE_TYPE,
        ARRANGE_BY_VIEW_COUNT, category.tourApiContentTypeId(), longitude, latitude, radiusMeters,
        MAX_CANDIDATES, 1);
    if (!response.isSuccess()) {
      throw new BusinessException(PlaceErrorCode.TOUR_API_RESPONSE_INVALID);
    }
    return response.itemsOrEmpty();
  }

  public Place findExistingByContentId(String contentId) {
    return placeRepository.findByContentId(contentId).orElse(null);
  }
}
