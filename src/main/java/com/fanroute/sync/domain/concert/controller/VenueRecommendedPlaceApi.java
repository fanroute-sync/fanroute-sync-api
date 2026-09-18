package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.concert.dto.VenueRecommendedPlaceDto;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/api/v1/venues/{venueId}/recommended-places")
@Tag(name = "공연장 추천 장소", description = "공연장별 추천 장소 조회 API")
public interface VenueRecommendedPlaceApi {

  @Operation(summary = "공연장별 추천 장소 조회",
      description = "concertScheduleId를 지정하면 공연 시간대와 겹치는 추천을 제외하고 가까운 "
          + "시간대순으로 정렬합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "추천 장소 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class,
      names = {"VENUE_NOT_FOUND", "SCHEDULE_NOT_FOUND", "SCHEDULE_VENUE_MISMATCH"})
  @GetMapping
  ResponseEntity<ApiResponse<List<VenueRecommendedPlaceDto.Response>>> getRecommendations(
      @Parameter(description = "공연장 ID") @PathVariable Long venueId,
      @Parameter(description = "공연 회차 ID(선택)")
      @RequestParam(required = false) Long concertScheduleId);
}
