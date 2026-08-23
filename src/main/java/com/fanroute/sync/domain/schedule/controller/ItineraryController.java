package com.fanroute.sync.domain.schedule.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.schedule.dto.ItineraryDto;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.service.ItineraryService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "일정", description = "날짜별 일정 편집 API")
@SecurityRequirement(name = "bearerAuth")
public class ItineraryController {
  private final CurrentUserResolver currentUserResolver;
  private final ItineraryService itineraryService;

  @Operation(summary = "날짜별 일정 조회")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "날짜별 일정 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class, names = "ITINERARY_DAY_NOT_FOUND")
  @GetMapping("/trip-plans/{tripPlanId}/itinerary-days/{date}")
  public ResponseEntity<ApiResponse<ItineraryDto.DayResponse>> getDay(@AuthenticationPrincipal Jwt jwt,
      @PathVariable Long tripPlanId, @PathVariable LocalDate date) {
    User user = currentUserResolver.getCurrentUser(jwt);
    return ApiResponse.ok(itineraryService.getDay(user, tripPlanId, date)).toResponseEntity();
  }
  @Operation(summary = "일정 항목 추가")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "일정 항목 추가 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class,
      names = {"ITINERARY_DAY_NOT_FOUND", "INVALID_ITINERARY_ITEM"})
  @PostMapping("/itinerary-days/{dayId}/items")
  public ResponseEntity<ApiResponse<ItineraryDto.ItemResponse>> addItem(@AuthenticationPrincipal Jwt jwt,
      @PathVariable Long dayId, @Valid @RequestBody ItineraryDto.CreateItemRequest request) {
    return ApiResponse.ok(itineraryService.addItem(currentUserResolver.getCurrentUser(jwt), dayId, request)).toResponseEntity();
  }
  @Operation(summary = "일정 항목 수정")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "일정 항목 수정 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class,
      names = {"ITINERARY_ITEM_NOT_FOUND", "INVALID_ITINERARY_ITEM", "FIXED_ITINERARY_ITEM"})
  @PatchMapping("/itinerary-items/{itemId}")
  public ResponseEntity<ApiResponse<ItineraryDto.ItemResponse>> updateItem(@AuthenticationPrincipal Jwt jwt,
      @PathVariable Long itemId, @Valid @RequestBody ItineraryDto.UpdateItemRequest request) {
    return ApiResponse.ok(itineraryService.updateItem(currentUserResolver.getCurrentUser(jwt), itemId, request)).toResponseEntity();
  }
  @Operation(summary = "일정 항목 삭제")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "일정 항목 삭제 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class,
      names = {"ITINERARY_ITEM_NOT_FOUND", "FIXED_ITINERARY_ITEM"})
  @DeleteMapping("/itinerary-items/{itemId}")
  public ResponseEntity<ApiResponse<Void>> deleteItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
    itineraryService.deleteItem(currentUserResolver.getCurrentUser(jwt), itemId);
    return ApiResponse.<Void>ok().toResponseEntity();
  }
  @Operation(summary = "일정 항목 순서 변경")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "일정 항목 순서 변경 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ScheduleErrorCode.class,
      names = {"ITINERARY_DAY_NOT_FOUND", "INVALID_ITINERARY_ITEM"})
  @PatchMapping("/itinerary-days/{dayId}/items/order")
  public ResponseEntity<ApiResponse<Void>> reorder(@AuthenticationPrincipal Jwt jwt, @PathVariable Long dayId,
      @Valid @RequestBody ItineraryDto.ReorderRequest request) {
    itineraryService.reorder(currentUserResolver.getCurrentUser(jwt), dayId, request);
    return ApiResponse.<Void>ok().toResponseEntity();
  }
}
