package com.fanroute.sync.domain.concert.service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.concert.dto.VenueRecommendedPlaceDto;
import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.entity.ConcertTimeSlot;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.entity.VenueRecommendedPlace;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.VenueRecommendedPlaceRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.service.PlaceService;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueRecommendedPlaceService {

  private final VenueRecommendedPlaceRepository repository;
  private final VenueRepository venueRepository;
  private final PlaceService placeService;
  private final ConcertScheduleService concertScheduleService;

  public VenueRecommendedPlace getRecommendation(Long recommendationId) {
    return repository.findById(recommendationId)
        .orElseThrow(() -> new BusinessException(ConcertErrorCode.RECOMMENDED_PLACE_NOT_FOUND));
  }

  public List<VenueRecommendedPlace> getRecommendations(
      Long venueId, Long concertScheduleId) {
    if (!venueRepository.existsById(venueId)) {
      throw new BusinessException(ConcertErrorCode.VENUE_NOT_FOUND);
    }
    List<VenueRecommendedPlace> recommendations =
        repository.findByVenueIdOrderBySortOrderAsc(venueId);
    if (concertScheduleId == null) {
      return recommendations;
    }
    ConcertSchedule schedule = concertScheduleService.getSchedule(concertScheduleId);
    validateVenueMatches(venueId, schedule);
    ConcertTimeSlot concertSlot = ConcertTimeSlot.fromTime(schedule.getPerformanceTime());
    return recommendations.stream()
        .filter(recommendation -> recommendation.getRecommendedTimeSlot() != concertSlot)
        .sorted(Comparator.comparingInt(
                (VenueRecommendedPlace recommendation) ->
                    timeSlotDistance(recommendation.getRecommendedTimeSlot(), concertSlot))
            .thenComparingInt(VenueRecommendedPlace::getSortOrder))
        .toList();
  }

  @Transactional
  public VenueRecommendedPlace create(VenueRecommendedPlaceDto.CreateRequest request) {
    Venue venue = venueRepository.findById(request.venueId())
        .orElseThrow(() -> new BusinessException(ConcertErrorCode.VENUE_NOT_FOUND));
    Place place = placeService.getPlace(request.placeId());
    return withConflictHandling(() -> repository.saveAndFlush(VenueRecommendedPlace.create(
        venue, place, request.sortOrder(), request.recommendedTimeSlot())));
  }

  /** 로컬에서 직접 만든 추천 장소 목록을 한 번에 등록합니다. 단건 등록과 같은 검증 경로를
   *  거치며, 하나라도 실패하면 전체가 롤백됩니다. */
  @Transactional
  public List<VenueRecommendedPlace> importRecommendations(
      List<VenueRecommendedPlaceDto.CreateRequest> requests) {
    return requests.stream().map(this::create).toList();
  }

  @Transactional
  public VenueRecommendedPlace update(
      Long recommendationId, VenueRecommendedPlaceDto.UpdateRequest request) {
    VenueRecommendedPlace recommendation = getRecommendation(recommendationId);
    Place place = placeService.getPlace(request.placeId());
    return withConflictHandling(() -> {
      recommendation.update(place, request.sortOrder(), request.recommendedTimeSlot());
      repository.flush();
      return recommendation;
    });
  }

  @Transactional
  public void delete(Long recommendationId) {
    repository.delete(getRecommendation(recommendationId));
  }

  private void validateVenueMatches(Long venueId, ConcertSchedule schedule) {
    if (!venueId.equals(schedule.getConcert().getVenue().getId())) {
      throw new BusinessException(ConcertErrorCode.SCHEDULE_VENUE_MISMATCH);
    }
  }

  private int timeSlotDistance(ConcertTimeSlot recommendationSlot, ConcertTimeSlot concertSlot) {
    return recommendationSlot == null
        ? 0 : Math.abs(recommendationSlot.ordinal() - concertSlot.ordinal());
  }

  private VenueRecommendedPlace withConflictHandling(Supplier<VenueRecommendedPlace> action) {
    try {
      return action.get();
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ConcertErrorCode.DUPLICATE_RECOMMENDED_PLACE);
    }
  }
}
