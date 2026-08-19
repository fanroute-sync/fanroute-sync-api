package com.fanroute.sync.domain.concert.controller;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.batch.JobRunResult;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/concerts")
@RequiredArgsConstructor
@Tag(name = "공연 관리", description = "관리자 전용 KOPIS 동기화 API")
@SecurityRequirement(name = "bearerAuth")
public class ConcertAdminController {

  private final JobOperator jobOperator;
  private final Job kopisConcertSyncJob;

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
  public ResponseEntity<ApiResponse<JobRunResult>> syncConcerts() {
    JobExecution execution = launch();
    return ApiResponse.ok(JobRunResult.from(execution)).toResponseEntity();
  }

  private JobExecution launch() {
    JobParameters jobParameters = new JobParametersBuilder()
        .addLong("triggeredAt", System.currentTimeMillis())
        .toJobParameters();
    try {
      return jobOperator.start(kopisConcertSyncJob, jobParameters);
    } catch (JobExecutionAlreadyRunningException | JobRestartException
        | JobInstanceAlreadyCompleteException | InvalidJobParametersException exception) {
      throw new BusinessException(ConcertErrorCode.SYNC_ALREADY_RUNNING);
    }
  }
}
