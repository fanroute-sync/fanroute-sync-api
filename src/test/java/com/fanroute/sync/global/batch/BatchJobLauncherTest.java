package com.fanroute.sync.global.batch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

class BatchJobLauncherTest {

  private final JobOperator jobOperator = mock(JobOperator.class);
  private final JobRepository jobRepository = mock(JobRepository.class);
  private final BatchJobLauncher batchJobLauncher =
      new BatchJobLauncher(jobOperator, jobRepository);

  @Test
  @DisplayName("동일한 이름의 Job이 실행 중이면 새 실행을 거부한다")
  void rejectsJobWithSameNameWhenRunning() {
    Job job = mock(Job.class);
    JobExecution runningExecution = mock(JobExecution.class);
    when(job.getName()).thenReturn("syncJob");
    when(jobRepository.findRunningJobExecutions("syncJob"))
        .thenReturn(Set.of(runningExecution));

    assertThatThrownBy(() -> batchJobLauncher.start(job, new JobParameters()))
        .isInstanceOf(JobExecutionAlreadyRunningException.class);

    verifyNoInteractions(jobOperator);
  }

  @Test
  @DisplayName("실행 중인 동일 Job이 없으면 새 실행을 시작한다")
  void startsJobWhenNotRunning() throws Exception {
    Job job = mock(Job.class);
    JobExecution execution = mock(JobExecution.class);
    JobParameters parameters = new JobParameters();
    when(job.getName()).thenReturn("syncJob");
    when(jobRepository.findRunningJobExecutions("syncJob")).thenReturn(Set.of());
    when(jobOperator.start(job, parameters)).thenReturn(execution);

    batchJobLauncher.start(job, parameters);

    verify(jobOperator).start(job, parameters);
  }
}
