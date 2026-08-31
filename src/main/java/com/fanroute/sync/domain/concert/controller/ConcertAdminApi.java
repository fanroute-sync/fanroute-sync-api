package com.fanroute.sync.domain.concert.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.batch.JobRunResult;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** {@link ConcertAdminController}의 Swagger 문서 계약. */
@RequestMapping("/api/v1/admin/concerts")
@Tag(name = "공연 관리", description = "관리자 전용 KOPIS 동기화 API")
@SecurityRequirement(name = "bearerAuth")
public interface ConcertAdminApi {

  @Operation(
      summary = "KOPIS 공연 수동 동기화",
      description = "ADMIN 권한을 가진 사용자만 KOPIS 공연·공연장 동기화 Job을 즉시 실행합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "동기화 실행 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = ConcertErrorCode.class,
      names = {"SYNC_ALREADY_RUNNING"})
  @ApiErrorCodeExamples(
      type = AuthErrorCode.class,
      names = {"ACCESS_DENIED"})
  @PostMapping("/sync")
  ResponseEntity<ApiResponse<JobRunResult>> syncConcerts();
}
