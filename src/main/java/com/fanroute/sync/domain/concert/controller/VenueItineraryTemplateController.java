package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.VenueItineraryTemplateDto;
import com.fanroute.sync.domain.concert.service.VenueItineraryTemplateService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class VenueItineraryTemplateController implements VenueItineraryTemplateApi {

  private final VenueItineraryTemplateService service;

  @Override
  public ResponseEntity<ApiResponse<List<VenueItineraryTemplateDto.Response>>> getTemplates(
      Long venueId) {
    return ApiResponse.ok(service.getTemplates(venueId)).toResponseEntity();
  }
}
