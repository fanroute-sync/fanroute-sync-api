package com.fanroute.sync.domain.concert.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.VenueItineraryTemplateDto;
import com.fanroute.sync.domain.concert.service.VenueItineraryTemplateService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class VenueItineraryTemplateInternalController
    implements VenueItineraryTemplateInternalApi {

  private final VenueItineraryTemplateService service;

  @Override
  public ResponseEntity<ApiResponse<VenueItineraryTemplateDto.Response>> importTemplate(
      VenueItineraryTemplateDto.ImportRequest request) {
    return ApiResponse.ok(service.importTemplate(request)).toResponseEntity();
  }
}
