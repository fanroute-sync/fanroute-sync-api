package com.fanroute.sync.domain.schedule.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;

import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.domain.schedule.dto.TripPlanDto;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RequestMapping("/api/v1/trip-plans/{tripPlanId}/accommodations")
@Tag(name = "숙소", description = "여행 계획 숙소 저장·조회 API")
@SecurityRequirement(name = "bearerAuth")
public interface AccommodationApi {

  @PutMapping
  @Operation(summary = "숙소 목록 저장", description = "숙소 목록 전체를 저장합니다. TourAPI 숙소는 저장된 좌표를 사용하고, 직접 입력은 카카오 로컬 API로 좌표를 조회합니다.")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "숙소 목록 저장 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class,
      names = {"TRIP_PLAN_NOT_FOUND", "INVALID_ACCOMMODATION"})
  @ApiErrorCodeExamples(type = PlaceErrorCode.class, names = "PLACE_NOT_FOUND")
  ResponseEntity<ApiResponse<List<TripPlanDto.AccommodationResponse>>> replace(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long tripPlanId,
      @Valid @RequestBody TripPlanDto.ReplaceAccommodationsRequest request);

  @GetMapping
  @Operation(summary = "숙소 목록 조회")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "숙소 목록 조회 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class, names = "TRIP_PLAN_NOT_FOUND")
  ResponseEntity<ApiResponse<List<TripPlanDto.AccommodationResponse>>> getAll(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long tripPlanId);

}
