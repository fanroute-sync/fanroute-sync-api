package com.fanroute.sync.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.service.AiItineraryGenerationService;
import com.fanroute.sync.domain.schedule.service.AiItineraryGenerationAsyncService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.response.SuccessCode;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "AI 일정", description = "AI 날짜별 일정 생성 작업 API")
@SecurityRequirement(name = "bearerAuth")
public class AiItineraryGenerationController {

  private final CurrentUserResolver currentUserResolver;
  private final AiItineraryGenerationService generationService;
  private final AiItineraryGenerationAsyncService generationAsyncService;

  @Operation(summary = "AI 일정 생성 작업 요청",
      description = "생성 작업을 접수한 뒤 백그라운드에서 실행합니다. 상태 조회 API로 완료 여부를 확인합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "202", description = "AI 일정 생성 작업 요청 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class, names = "ITINERARY_DAY_NOT_FOUND")
  @PostMapping("/itinerary-days/{itineraryDayId}/ai-generations")
  public ResponseEntity<ApiResponse<AiItineraryGenerationDto.CreateResponse>> requestGeneration(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long itineraryDayId) {
    User user = currentUserResolver.getCurrentUser(jwt);
    AiItineraryGenerationDto.CreateResponse response = generationService.request(user, itineraryDayId);
    generationAsyncService.generate(response.generationId());
    return ApiResponse.of(SuccessCode.ACCEPTED, response).toResponseEntity();
  }

  @Operation(summary = "AI 일정 생성 작업 상태 조회")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "AI 일정 생성 작업 상태 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class, names = "AI_ITINERARY_GENERATION_NOT_FOUND")
  @GetMapping("/ai-itinerary-generations/{generationId}")
  public ResponseEntity<ApiResponse<AiItineraryGenerationDto.StatusResponse>> getGenerationStatus(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long generationId) {
    User user = currentUserResolver.getCurrentUser(jwt);
    return ApiResponse.ok(generationService.getStatus(user, generationId)).toResponseEntity();
  }

  @Operation(summary = "실패한 AI 일정 생성 작업 재시도",
      description = "새 생성 작업을 접수한 뒤 백그라운드에서 실행합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "202", description = "AI 일정 생성 작업 재시도 접수 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class,
      names = {"AI_ITINERARY_GENERATION_NOT_FOUND", "INVALID_AI_ITINERARY_GENERATION_STATUS"})
  @PostMapping("/ai-itinerary-generations/{generationId}/retry")
  public ResponseEntity<ApiResponse<AiItineraryGenerationDto.CreateResponse>> retryGeneration(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long generationId) {
    User user = currentUserResolver.getCurrentUser(jwt);
    AiItineraryGenerationDto.CreateResponse response = generationService.retry(user, generationId);
    generationAsyncService.generate(response.generationId());
    return ApiResponse.of(SuccessCode.ACCEPTED, response)
        .toResponseEntity();
  }

  @Operation(summary = "대기 중인 AI 일정 생성 작업 취소")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "AI 일정 생성 작업 취소 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class,
      names = {"AI_ITINERARY_GENERATION_NOT_FOUND", "INVALID_AI_ITINERARY_GENERATION_STATUS"})
  @DeleteMapping("/ai-itinerary-generations/{generationId}")
  public ResponseEntity<ApiResponse<AiItineraryGenerationDto.StatusResponse>> cancelGeneration(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long generationId) {
    User user = currentUserResolver.getCurrentUser(jwt);
    return ApiResponse.ok(generationService.cancel(user, generationId)).toResponseEntity();
  }
}
