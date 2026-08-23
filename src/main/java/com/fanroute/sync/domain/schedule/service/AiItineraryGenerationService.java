package com.fanroute.sync.domain.schedule.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.AiItineraryGenerationRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryDayRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AiItineraryGenerationService {

  private final AiItineraryGenerationRepository generationRepository;
  private final ItineraryDayRepository itineraryDayRepository;

  public AiItineraryGenerationDto.CreateResponse request(User user, Long itineraryDayId) {
    ItineraryDay itineraryDay = getOwnedItineraryDay(user, itineraryDayId);
    AiItineraryGeneration generation = generationRepository.save(
        AiItineraryGeneration.create(itineraryDay));
    return AiItineraryGenerationDto.CreateResponse.from(generation);
  }

  @Transactional(readOnly = true)
  public AiItineraryGenerationDto.StatusResponse getStatus(User user, Long generationId) {
    AiItineraryGeneration generation = generationRepository
        .findByIdAndItineraryDayTripPlanUserId(generationId, user.getId())
        .orElseThrow(() -> new BusinessException(
            ScheduleErrorCode.AI_ITINERARY_GENERATION_NOT_FOUND));
    return AiItineraryGenerationDto.StatusResponse.from(generation);
  }

  private ItineraryDay getOwnedItineraryDay(User user, Long itineraryDayId) {
    return itineraryDayRepository.findById(itineraryDayId)
        .filter(day -> day.getTripPlan().getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new BusinessException(ScheduleErrorCode.ITINERARY_DAY_NOT_FOUND));
  }
}
