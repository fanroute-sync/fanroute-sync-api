package com.fanroute.sync.domain.schedule.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.schedule.dto.TripPlanDto;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.TripPlanRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TravelStyleService {

  private final TripPlanRepository tripPlanRepository;

  @Transactional(readOnly = true)
  public TripPlanDto.TravelStyleResponse get(User user, Long tripPlanId) {
    return toResponse(getOwnedTripPlan(user, tripPlanId));
  }

  public TripPlanDto.TravelStyleResponse update(User user, Long tripPlanId,
      TripPlanDto.UpdateTravelStyleRequest request) {
    TripPlan tripPlan = getOwnedTripPlan(user, tripPlanId);
    tripPlan.updateTravelStyle(request.travelIntensity(), request.companions(), request.travelMbti());
    return toResponse(tripPlan);
  }

  private TripPlan getOwnedTripPlan(User user, Long tripPlanId) {
    return tripPlanRepository.findByIdAndUserId(tripPlanId, user.getId())
        .orElseThrow(() -> new BusinessException(ScheduleErrorCode.TRIP_PLAN_NOT_FOUND));
  }

  private TripPlanDto.TravelStyleResponse toResponse(TripPlan tripPlan) {
    return new TripPlanDto.TravelStyleResponse(tripPlan.getTravelIntensity(),
        tripPlan.getCompanions(), tripPlan.getTravelMbti());
  }
}
