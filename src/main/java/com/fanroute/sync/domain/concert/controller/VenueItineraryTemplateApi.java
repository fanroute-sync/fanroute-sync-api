package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fanroute.sync.domain.concert.dto.VenueItineraryTemplateDto;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/api/v1/venues/{venueId}/itinerary-templates")
@Tag(name = "공연장 일정 템플릿", description = "공연장별 일정 초안 조회 API")
public interface VenueItineraryTemplateApi {

  @Operation(summary = "공연장 일정 템플릿 조회",
      description = "공연을 제외한 장소 일정 초안을 조회합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "템플릿 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "VENUE_NOT_FOUND")
  @GetMapping
  ResponseEntity<ApiResponse<List<VenueItineraryTemplateDto.Response>>> getTemplates(
      @Parameter(description = "공연장 ID") @PathVariable Long venueId);
}
