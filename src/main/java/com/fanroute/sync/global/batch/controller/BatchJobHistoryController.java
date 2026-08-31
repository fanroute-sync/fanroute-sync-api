package com.fanroute.sync.global.batch.controller;

import java.util.List;

import org.springframework.batch.core.job.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.global.batch.BatchJobExecutionRepository;
import com.fanroute.sync.global.batch.JobRunResult;
import com.fanroute.sync.global.batch.exception.BatchErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BatchJobHistoryController implements BatchJobHistoryApi {

  private final BatchJobExecutionRepository batchJobExecutionRepository;
  private final List<Job> jobs;

  @Override
  public ResponseEntity<ApiResponse<Page<JobRunResult>>> getExecutions(
      String jobName, int page, int size) {
    ensureJobExists(jobName);

    Pageable pageable = toPageable(page, size);
    Page<JobRunResult> result = batchJobExecutionRepository.findByJobName(jobName, pageable);
    return ApiResponse.ok(result).toResponseEntity();
  }

  private void ensureJobExists(String jobName) {
    boolean registered = jobs.stream().anyMatch(job -> job.getName().equals(jobName));
    if (!registered) {
      throw new BusinessException(BatchErrorCode.JOB_NOT_FOUND);
    }
  }

  private Pageable toPageable(int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    return PageRequest.of(safePage, safeSize);
  }
}
