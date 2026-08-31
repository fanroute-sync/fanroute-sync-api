package com.fanroute.sync.global.batch.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.global.batch.JobRunResult;
import com.fanroute.sync.global.batch.exception.BatchErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * {@link BatchJobHistoryController}의 Swagger 문서 계약.
 * <p>ADMIN Role은 SecurityConfig의 {@code /api/v1/admin/**} 매칭으로 검증됩니다.</p>
 */
@RequestMapping("/api/v1/admin/jobs")
@Tag(name = "Batch Job 이력", description = "관리자 전용 동기화 Job 실행 이력 조회 API")
@SecurityRequirement(name = "bearerAuth")
public interface BatchJobHistoryApi {

  int MAX_PAGE_SIZE = 100;

  @Operation(
      summary = "Job 실행 이력 조회",
      description = "지정한 Job의 실행 이력을 최근 실행순으로 조회합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "실행 이력 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = BatchErrorCode.class, names = "JOB_NOT_FOUND")
  @ApiErrorCodeExamples(
      type = AuthErrorCode.class,
      names = {"AUTHENTICATION_REQUIRED", "ACCESS_DENIED"})
  @GetMapping("/{jobName}/executions")
  ResponseEntity<ApiResponse<Page<JobRunResult>>> getExecutions(
      @Parameter(
          description = "실행 이력을 조회할 Job 이름. kopisConcertSyncJob, accommodationPlaceSyncJob, "
              + "attractionPlaceSyncJob, restaurantPlaceSyncJob 중 하나입니다.",
          example = "kopisConcertSyncJob")
      @PathVariable String jobName,
      @Parameter(description = "페이지 번호(0부터 시작)")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "페이지 크기(최대 " + MAX_PAGE_SIZE + ")")
      @RequestParam(defaultValue = "20") int size);
}
