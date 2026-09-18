package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.VenueRecommendedPlaceDto;
import com.fanroute.sync.domain.concert.service.VenueRecommendedPlaceService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class VenueRecommendedPlaceController implements VenueRecommendedPlaceApi {

  private final VenueRecommendedPlaceService service;

  @Override
  public ResponseEntity<ApiResponse<List<VenueRecommendedPlaceDto.Response>>> getRecommendations(
      Long venueId, Long concertScheduleId) {
    List<VenueRecommendedPlaceDto.Response> responses = service
        .getRecommendations(venueId, concertScheduleId).stream()
        .map(VenueRecommendedPlaceDto.Response::from)
        .toList();
    return ApiResponse.ok(responses).toResponseEntity();
  }
}
