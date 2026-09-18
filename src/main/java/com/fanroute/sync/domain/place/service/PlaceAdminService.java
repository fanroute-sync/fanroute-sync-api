package com.fanroute.sync.domain.place.service;

import java.util.List;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.place.client.TourApiClient;
import com.fanroute.sync.domain.place.config.TourApiProperties;
import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.dto.PlaceDto;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.VenueRepository;
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
  private final VenueRepository venueRepository;

  public List<TourApiDto.PlaceSummary> searchNearbyCandidates(
      Long venueId, PlaceCategory category, int radiusMeters) {
    Venue venue = venueRepository.findById(venueId)
        .orElseThrow(() -> new BusinessException(ConcertErrorCode.VENUE_NOT_FOUND));
    if (venue.getLatitude() == null || venue.getLongitude() == null) {
      throw new BusinessException(ConcertErrorCode.VENUE_LOCATION_NOT_FOUND);
    }
    return searchNearbyCandidates(category, venue.getLatitude(), venue.getLongitude(), radiusMeters);
  }

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

  @Transactional
  public List<Place> importCandidates(
      PlaceCategory category, List<PlaceDto.CandidateImportRequest> candidates) {
    return candidates.stream().map(candidate -> importCandidate(category, candidate)).toList();
  }

  private Place importCandidate(PlaceCategory category, PlaceDto.CandidateImportRequest candidate) {
    return placeRepository.findByContentId(candidate.contentId())
        .map(place -> {
          place.updateFromSync(category, category.tourApiContentTypeId(), candidate.name(),
              candidate.address(), candidate.detailAddress(), candidate.zipCode(),
              candidate.latitude(), candidate.longitude(), candidate.telephone(),
              candidate.imageUrl(), candidate.thumbnailUrl(), candidate.copyrightType(),
              null, null, null, null, null, null, null, Instant.now());
          return place;
        })
        .orElseGet(() -> placeRepository.save(Place.create(
            candidate.contentId(), category, category.tourApiContentTypeId(), candidate.name(),
            candidate.address(), candidate.detailAddress(), candidate.zipCode(),
            candidate.latitude(), candidate.longitude(), candidate.telephone(), candidate.imageUrl(),
            candidate.thumbnailUrl(), candidate.copyrightType(), null, null, null, null, null,
            null, null, Instant.now())));
  }
}
