package com.fanroute.sync.domain.concert.service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.concert.dto.RecommendationTemplateDto;
import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.entity.ConcertTimeSlot;
import com.fanroute.sync.domain.concert.entity.RecommendationTemplate;
import com.fanroute.sync.domain.concert.entity.RecommendationTemplatePlace;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.entity.VenueRecommendedPlace;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.RecommendationTemplatePlaceRepository;
import com.fanroute.sync.domain.concert.repository.RecommendationTemplateRepository;
import com.fanroute.sync.domain.concert.repository.VenueRecommendedPlaceRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationTemplateService {

  private final RecommendationTemplateRepository templateRepository;
  private final RecommendationTemplatePlaceRepository templatePlaceRepository;
  private final VenueRecommendedPlaceRepository recommendedPlaceRepository;
  private final VenueRepository venueRepository;
  private final ConcertScheduleService concertScheduleService;

  public RecommendationTemplate getTemplate(Long templateId) {
    return templateRepository.findById(templateId)
        .orElseThrow(() -> new BusinessException(ConcertErrorCode.RECOMMENDATION_TEMPLATE_NOT_FOUND));
  }

  public List<RecommendationTemplate> getTemplates(Long venueId) {
    if (!venueRepository.existsById(venueId)) {
      throw new BusinessException(ConcertErrorCode.VENUE_NOT_FOUND);
    }
    return templateRepository.findByVenueIdOrderByIdAsc(venueId);
  }

  public List<RecommendationTemplatePlace> getPlaces(Long templateId) {
    return templatePlaceRepository.findByTemplateIdOrderBySortOrderAsc(templateId);
  }

  public List<RecommendationTemplatePlace> getPlaces(Long templateId, Long concertScheduleId) {
    List<RecommendationTemplatePlace> places = getPlaces(templateId);
    if (concertScheduleId == null) {
      return places;
    }
    RecommendationTemplate template = getTemplate(templateId);
    ConcertSchedule schedule = concertScheduleService.getSchedule(concertScheduleId);
    if (!template.getVenue().getId().equals(schedule.getConcert().getVenue().getId())) {
      throw new BusinessException(ConcertErrorCode.SCHEDULE_VENUE_MISMATCH);
    }
    ConcertTimeSlot concertSlot = ConcertTimeSlot.fromTime(schedule.getPerformanceTime());
    return places.stream()
        .filter(place -> place.getRecommendedPlace().getRecommendedTimeSlot() != concertSlot)
        .sorted(Comparator.comparingInt((RecommendationTemplatePlace place) -> distance(
            place.getRecommendedPlace().getRecommendedTimeSlot(), concertSlot))
            .thenComparingInt(RecommendationTemplatePlace::getSortOrder))
        .toList();
  }

  @Transactional
  public RecommendationTemplate create(RecommendationTemplateDto.CreateTemplateRequest request) {
    Venue venue = venueRepository.findById(request.venueId())
        .orElseThrow(() -> new BusinessException(ConcertErrorCode.VENUE_NOT_FOUND));
    return templateRepository.save(RecommendationTemplate.create(venue, request.name(), request.description()));
  }

  @Transactional
  public RecommendationTemplate update(Long templateId,
      RecommendationTemplateDto.UpdateTemplateRequest request) {
    RecommendationTemplate template = getTemplate(templateId);
    template.update(request.name(), request.description());
    return template;
  }

  @Transactional
  public void delete(Long templateId) {
    getTemplate(templateId);
    templatePlaceRepository.deleteByTemplateId(templateId);
    templateRepository.deleteById(templateId);
  }

  @Transactional
  public RecommendationTemplatePlace addPlace(Long templateId,
      RecommendationTemplateDto.AddPlaceRequest request) {
    RecommendationTemplate template = getTemplate(templateId);
    VenueRecommendedPlace place = recommendedPlaceRepository.findById(request.recommendedPlaceId())
        .orElseThrow(() -> new BusinessException(ConcertErrorCode.RECOMMENDED_PLACE_NOT_FOUND));
    if (!template.getVenue().getId().equals(place.getVenue().getId())) {
      throw new BusinessException(ConcertErrorCode.RECOMMENDED_PLACE_VENUE_MISMATCH);
    }
    return withConflictHandling(() -> templatePlaceRepository.saveAndFlush(
        RecommendationTemplatePlace.create(template, place, request.sortOrder())));
  }

  @Transactional
  public void removePlace(Long templatePlaceId) {
    if (!templatePlaceRepository.existsById(templatePlaceId)) {
      throw new BusinessException(ConcertErrorCode.RECOMMENDATION_TEMPLATE_PLACE_NOT_FOUND);
    }
    templatePlaceRepository.deleteById(templatePlaceId);
  }

  private int distance(ConcertTimeSlot recommendationSlot, ConcertTimeSlot concertSlot) {
    return recommendationSlot == null ? 0 : Math.abs(recommendationSlot.ordinal() - concertSlot.ordinal());
  }

  private RecommendationTemplatePlace withConflictHandling(
      Supplier<RecommendationTemplatePlace> action) {
    try {
      return action.get();
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ConcertErrorCode.DUPLICATE_RECOMMENDATION_TEMPLATE_PLACE);
    }
  }
}
