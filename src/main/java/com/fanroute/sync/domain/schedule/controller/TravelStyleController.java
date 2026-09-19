package com.fanroute.sync.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.schedule.dto.TripPlanDto;
import com.fanroute.sync.domain.schedule.service.TravelStyleService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TravelStyleController implements TravelStyleApi {

  private final CurrentUserResolver currentUserResolver;
  private final TravelStyleService travelStyleService;

  @Override
  public ResponseEntity<ApiResponse<TripPlanDto.TravelStyleResponse>> get(Jwt jwt, Long tripPlanId) {
    User user = currentUserResolver.getCurrentUser(jwt);
    return ApiResponse.ok(travelStyleService.get(user, tripPlanId)).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<TripPlanDto.TravelStyleResponse>> update(Jwt jwt,
      Long tripPlanId, TripPlanDto.UpdateTravelStyleRequest request) {
    User user = currentUserResolver.getCurrentUser(jwt);
    return ApiResponse.ok(travelStyleService.update(user, tripPlanId, request)).toResponseEntity();
  }
}
