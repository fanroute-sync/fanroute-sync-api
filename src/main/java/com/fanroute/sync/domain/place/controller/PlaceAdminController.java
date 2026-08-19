package com.fanroute.sync.domain.place.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.domain.place.service.PlaceSyncService;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;
import com.fanroute.sync.global.config.AdminProperties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 역할 기반 인가를 도입하기 전까지 고정 관리자 키로 보호합니다. */
@RestController
@RequestMapping("/api/v1/admin/places")
@RequiredArgsConstructor
@Tag(name = "장소 관리", description = "관리자 전용 TourAPI 동기화 API")
public class PlaceAdminController {

  private static final String ADMIN_KEY_HEADER = "X-Admin-Key";

  private final PlaceSyncService placeSyncService;
  private final AdminProperties adminProperties;

  @Operation(
      summary = "TourAPI 숙박 장소 수동 동기화",
      description = "관리자 키로 인증된 요청만 TourAPI 숙박 장소 동기화를 즉시 실행합니다. "
          + "1차 구현은 숙박(ACCOMMODATION)만 지원합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "동기화 실행 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = PlaceErrorCode.class,
      names = {"ADMIN_ACCESS_DENIED", "TOUR_API_UNAVAILABLE", "TOUR_API_RESPONSE_INVALID"})
  @Parameter(
      name = ADMIN_KEY_HEADER, description = "관리자 전용 API 키", in = ParameterIn.HEADER,
      required = true)
  @PostMapping("/sync")
  public ResponseEntity<ApiResponse<PlaceSyncService.SyncResult>> syncAccommodations(
      @Parameter(hidden = true)
      @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String adminKey) {
    validateAdminKey(adminKey);
    PlaceSyncService.SyncResult result = placeSyncService.sync(PlaceCategory.ACCOMMODATION);
    return ApiResponse.ok(result).toResponseEntity();
  }

  private void validateAdminKey(String adminKey) {
    if (adminKey == null || !adminProperties.apiKey().equals(adminKey)) {
      throw new BusinessException(PlaceErrorCode.ADMIN_ACCESS_DENIED);
    }
  }
}
