package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.VenueRecommendedPlaceDto;
import com.fanroute.sync.domain.concert.entity.VenueRecommendedPlace;
import com.fanroute.sync.domain.concert.service.VenueRecommendedPlaceService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class VenueRecommendedPlaceAdminController implements VenueRecommendedPlaceAdminApi {

  private final VenueRecommendedPlaceService service;

  @Override
  public ResponseEntity<ApiResponse<List<VenueRecommendedPlaceDto.Response>>> importRecommendations(
      VenueRecommendedPlaceDto.BulkImportRequest request) {
    List<VenueRecommendedPlace> recommendations =
        service.importRecommendations(request.recommendations());
    List<VenueRecommendedPlaceDto.Response> responses = recommendations.stream()
        .map(VenueRecommendedPlaceDto.Response::from)
        .toList();
    return ApiResponse.ok(responses).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<VenueRecommendedPlaceDto.Response>> create(
      VenueRecommendedPlaceDto.CreateRequest request) {
    return ApiResponse.ok(VenueRecommendedPlaceDto.Response.from(service.create(request)))
        .toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<VenueRecommendedPlaceDto.Response>> update(
      Long recommendationId, VenueRecommendedPlaceDto.UpdateRequest request) {
    return ApiResponse.ok(
        VenueRecommendedPlaceDto.Response.from(service.update(recommendationId, request)))
        .toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> delete(Long recommendationId) {
    service.delete(recommendationId);
    return ApiResponse.<Void>ok().toResponseEntity();
  }
}
