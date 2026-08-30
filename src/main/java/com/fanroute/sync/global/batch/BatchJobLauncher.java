package com.fanroute.sync.global.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** 같은 애플리케이션 인스턴스에서 동일한 이름의 Job이 동시에 시작되지 않도록 직렬화합니다. */
@Component
@RequiredArgsConstructor
public class BatchJobLauncher {

  private final JobOperator jobOperator;
  private final JobRepository jobRepository;

  /** 실행 중 여부 확인과 시작을 하나의 임계 구역에서 수행합니다. */
  public synchronized JobExecution start(Job job, JobParameters parameters)
      throws JobExecutionAlreadyRunningException, JobRestartException,
      JobInstanceAlreadyCompleteException, InvalidJobParametersException {
    if (!jobRepository.findRunningJobExecutions(job.getName()).isEmpty()) {
      throw new JobExecutionAlreadyRunningException(
          "Job is already running: " + job.getName());
    }
    return jobOperator.start(job, parameters);
  }
}
