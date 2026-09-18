package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.RecommendationTemplateDto;
import com.fanroute.sync.domain.concert.service.RecommendationTemplateService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RecommendationTemplateController implements RecommendationTemplateApi {

  private final RecommendationTemplateService service;

  @Override
  public ResponseEntity<ApiResponse<List<RecommendationTemplateDto.Response>>> getTemplates(
      Long venueId, Long concertScheduleId) {
    List<RecommendationTemplateDto.Response> responses = service.getTemplates(venueId).stream()
        .map(template -> RecommendationTemplateDto.Response.from(
            template, service.getPlaces(template.getId(), concertScheduleId)))
        .toList();
    return ApiResponse.ok(responses).toResponseEntity();
  }
}
