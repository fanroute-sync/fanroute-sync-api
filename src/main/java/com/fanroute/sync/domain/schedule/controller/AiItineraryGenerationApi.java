package com.fanroute.sync.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** {@link AiItineraryGenerationController}의 Swagger 문서 계약. */
@RequestMapping("/api/v1")
@Tag(name = "AI 일정", description = "AI 날짜별 일정 생성 작업 API")
@SecurityRequirement(name = "bearerAuth")
public interface AiItineraryGenerationApi {

  @Operation(summary = "AI 일정 생성 작업 요청",
      description = "생성 작업을 접수한 뒤 백그라운드에서 실행합니다. 상태 조회 API로 완료 여부를 확인합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "202", description = "AI 일정 생성 작업 요청 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class,
      names = {"ITINERARY_DAY_NOT_FOUND", "AI_ITINERARY_GENERATION_UNAVAILABLE_ON_CONCERT_DAY",
          "AI_ITINERARY_GENERATION_LIMIT_EXCEEDED"})
  @PostMapping("/itinerary-days/{itineraryDayId}/ai-generations")
  ResponseEntity<ApiResponse<AiItineraryGenerationDto.CreateResponse>> requestGeneration(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long itineraryDayId);

  @Operation(summary = "AI 일정 생성 작업 상태 조회")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "AI 일정 생성 작업 상태 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class, names = "AI_ITINERARY_GENERATION_NOT_FOUND")
  @GetMapping("/ai-itinerary-generations/{generationId}")
  ResponseEntity<ApiResponse<AiItineraryGenerationDto.StatusResponse>> getGenerationStatus(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long generationId);

  @Operation(summary = "실패한 AI 일정 생성 작업 재시도",
      description = "새 생성 작업을 접수한 뒤 백그라운드에서 실행합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "202", description = "AI 일정 생성 작업 재시도 접수 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class,
      names = {"AI_ITINERARY_GENERATION_NOT_FOUND", "INVALID_AI_ITINERARY_GENERATION_STATUS",
          "AI_ITINERARY_GENERATION_UNAVAILABLE_ON_CONCERT_DAY",
          "AI_ITINERARY_GENERATION_LIMIT_EXCEEDED"})
  @PostMapping("/ai-itinerary-generations/{generationId}/retry")
  ResponseEntity<ApiResponse<AiItineraryGenerationDto.CreateResponse>> retryGeneration(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long generationId);

  @Operation(summary = "대기 중인 AI 일정 생성 작업 취소")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "AI 일정 생성 작업 취소 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class,
      names = {"AI_ITINERARY_GENERATION_NOT_FOUND", "INVALID_AI_ITINERARY_GENERATION_STATUS"})
  @DeleteMapping("/ai-itinerary-generations/{generationId}")
  ResponseEntity<ApiResponse<AiItineraryGenerationDto.StatusResponse>> cancelGeneration(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long generationId);
}
