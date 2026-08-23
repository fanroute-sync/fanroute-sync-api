package com.fanroute.sync.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.schedule.dto.TripPlanDto;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.service.TripPlanService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.response.SuccessCode;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/trip-plans")
@RequiredArgsConstructor
@Tag(name = "일정", description = "여행 일정 생성 API")
@SecurityRequirement(name = "bearerAuth")
public class TripPlanController {

  private final CurrentUserResolver currentUserResolver;
  private final TripPlanService tripPlanService;

  @Operation(summary = "여행 일정 생성", description = "도착·출발 날짜와 시간대, 선택 공연으로 여행 계획을 생성합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "201", description = "여행 일정 생성 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class, names = "INVALID_TRIP_PERIOD")
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "CONCERT_NOT_FOUND")
  @PostMapping
  public ResponseEntity<ApiResponse<TripPlanDto.CreateResponse>> createTripPlan(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TripPlanDto.CreateRequest request) {
    User user = currentUserResolver.getCurrentUser(jwt);
    TripPlanDto.CreateResponse response = tripPlanService.create(user, request);
    return ApiResponse.of(SuccessCode.CREATED, response).toResponseEntity();
  }
}
