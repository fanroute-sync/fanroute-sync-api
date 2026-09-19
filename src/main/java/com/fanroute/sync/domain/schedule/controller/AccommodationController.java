package com.fanroute.sync.domain.schedule.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.schedule.dto.TripPlanDto;
import com.fanroute.sync.domain.schedule.service.AccommodationService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AccommodationController implements AccommodationApi {

  private final CurrentUserResolver currentUserResolver;
  private final AccommodationService accommodationService;

  @Override
  public ResponseEntity<ApiResponse<List<TripPlanDto.AccommodationResponse>>> replace(Jwt jwt,
      Long tripPlanId, TripPlanDto.ReplaceAccommodationsRequest request) {
    User user = currentUserResolver.getCurrentUser(jwt);
    return ApiResponse.ok(accommodationService.replace(user, tripPlanId, request)).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<List<TripPlanDto.AccommodationResponse>>> getAll(Jwt jwt,
      Long tripPlanId) {
    User user = currentUserResolver.getCurrentUser(jwt);
    return ApiResponse.ok(accommodationService.getAll(user, tripPlanId)).toResponseEntity();
  }
}
