package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.concert.dto.RecommendationTemplateDto;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/api/v1/venues/{venueId}/recommendation-templates")
@Tag(name = "추천 코스", description = "공연장별 이름 있는 추천 코스 조회 API")
public interface RecommendationTemplateApi {

  @Operation(summary = "공연장 추천 코스 조회")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "추천 코스 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class,
      names = {"VENUE_NOT_FOUND", "RECOMMENDATION_TEMPLATE_NOT_FOUND", "SCHEDULE_NOT_FOUND",
          "SCHEDULE_VENUE_MISMATCH"})
  @GetMapping
  ResponseEntity<ApiResponse<List<RecommendationTemplateDto.Response>>> getTemplates(
      @Parameter(description = "공연장 ID") @PathVariable Long venueId,
      @Parameter(description = "공연 회차 ID(선택)")
      @RequestParam(required = false) Long concertScheduleId);
}
