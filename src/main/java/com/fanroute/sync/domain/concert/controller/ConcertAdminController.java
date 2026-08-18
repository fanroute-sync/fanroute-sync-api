package com.fanroute.sync.domain.concert.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.service.ConcertSyncService;
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
@RequestMapping("/api/v1/admin/concerts")
@RequiredArgsConstructor
@Tag(name = "공연 관리", description = "관리자 전용 KOPIS 동기화 API")
public class ConcertAdminController {

  private static final String ADMIN_KEY_HEADER = "X-Admin-Key";

  private final ConcertSyncService concertSyncService;
  private final AdminProperties adminProperties;

  @Operation(
      summary = "KOPIS 공연 수동 동기화",
      description = "관리자 키로 인증된 요청만 KOPIS 공연·공연장 동기화를 즉시 실행합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "동기화 실행 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = ConcertErrorCode.class,
      names = {"ADMIN_ACCESS_DENIED", "KOPIS_API_UNAVAILABLE", "KOPIS_RESPONSE_INVALID"})
  @Parameter(
      name = ADMIN_KEY_HEADER, description = "관리자 전용 API 키", in = ParameterIn.HEADER,
      required = true)
  @PostMapping("/sync")
  public ResponseEntity<ApiResponse<ConcertSyncService.SyncResult>> syncConcerts(
      @Parameter(hidden = true)
      @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String adminKey) {
    validateAdminKey(adminKey);
    ConcertSyncService.SyncResult result = concertSyncService.syncConcerts();
    return ApiResponse.ok(result).toResponseEntity();
  }

  private void validateAdminKey(String adminKey) {
    if (adminKey == null || !adminProperties.apiKey().equals(adminKey)) {
      throw new BusinessException(ConcertErrorCode.ADMIN_ACCESS_DENIED);
    }
  }
}
