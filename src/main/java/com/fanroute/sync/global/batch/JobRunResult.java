package com.fanroute.sync.global.batch;

import java.time.LocalDateTime;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;

import io.swagger.v3.oas.annotations.media.Schema;

/** Batch Job에 포함된 Step의 처리 건수를 합산합니다. */
public record JobRunResult(
    @Schema(description = "Job 이름") String jobName,
    @Schema(description = "실행 상태(COMPLETED/FAILED 등)") String status,
    @Schema(description = "종료 코드") String exitCode,
    @Schema(description = "읽은 건수") long readCount,
    @Schema(description = "저장한 건수") long writeCount,
    @Schema(description = "건너뛴 건수") long skipCount,
    @Schema(description = "시작 시각") LocalDateTime startTime,
    @Schema(description = "종료 시각") LocalDateTime endTime) {

  /** JobExecution 없이 실행 자체가 실패한 경우를 표현합니다. */
  public static JobRunResult failed(String jobName, String reasonCode) {
    return new JobRunResult(jobName, "LAUNCH_FAILED", reasonCode, 0, 0, 0, null, null);
  }

  public static JobRunResult from(JobExecution execution) {
    long readCount = 0;
    long writeCount = 0;
    long skipCount = 0;
    for (StepExecution stepExecution : execution.getStepExecutions()) {
      readCount += stepExecution.getReadCount();
      writeCount += stepExecution.getWriteCount();
      skipCount += stepExecution.getSkipCount();
    }
    return new JobRunResult(
        execution.getJobInstance().getJobName(),
        execution.getStatus().toString(),
        execution.getExitStatus().getExitCode(),
        readCount, writeCount, skipCount,
        execution.getStartTime(), execution.getEndTime());
  }
}
