package com.fanroute.sync.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fanroute.sync.domain.schedule.dto.TripPlanDto;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RequestMapping("/api/v1/trip-plans/{tripPlanId}/travel-style")
@Tag(name = "여행 스타일", description = "여행 강도·동행·여행 MBTI 설정 API")
@SecurityRequirement(name = "bearerAuth")
public interface TravelStyleApi {

  @GetMapping
  @Operation(summary = "여행 스타일 조회")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "여행 스타일 조회 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class, names = "TRIP_PLAN_NOT_FOUND")
  ResponseEntity<ApiResponse<TripPlanDto.TravelStyleResponse>> get(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long tripPlanId);

  @PutMapping
  @Operation(summary = "여행 스타일 저장", description = "여행 강도, 복수 동행, 여행 MBTI를 저장합니다.")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "여행 스타일 저장 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class, names = "TRIP_PLAN_NOT_FOUND")
  ResponseEntity<ApiResponse<TripPlanDto.TravelStyleResponse>> update(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long tripPlanId,
      @Valid @RequestBody TripPlanDto.UpdateTravelStyleRequest request);
}
