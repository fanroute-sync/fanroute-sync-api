package com.fanroute.sync.domain.concert.controller;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.batch.JobRunResult;
import com.fanroute.sync.global.batch.BatchJobLauncher;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ConcertAdminController implements ConcertAdminApi {

  private final BatchJobLauncher batchJobLauncher;
  private final Job kopisConcertSyncJob;

  @Override
  public ResponseEntity<ApiResponse<JobRunResult>> syncConcerts() {
    JobExecution execution = launch();
    return ApiResponse.ok(JobRunResult.from(execution)).toResponseEntity();
  }

  private JobExecution launch() {
    JobParameters jobParameters = new JobParametersBuilder()
        .addLong("triggeredAt", System.currentTimeMillis())
        .toJobParameters();
    try {
      return batchJobLauncher.start(kopisConcertSyncJob, jobParameters);
    } catch (JobExecutionAlreadyRunningException | JobRestartException
        | JobInstanceAlreadyCompleteException | InvalidJobParametersException exception) {
      throw new BusinessException(ConcertErrorCode.SYNC_ALREADY_RUNNING);
    }
  }
}
