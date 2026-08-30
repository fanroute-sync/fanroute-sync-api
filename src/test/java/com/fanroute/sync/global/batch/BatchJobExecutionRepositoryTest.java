package com.fanroute.sync.global.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.fanroute.sync.support.TestContainerConfig;

@SpringBootTest(properties = {
    "auth.google.client-id=test-google-client",
    "auth.google.client-secret=test-google-secret",
    "auth.google.redirect-uri=http://localhost/test/callback",
    "auth.jwt.secret=dGVzdC1vbmx5LWtleS10aGF0LWlzLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=",
    "kopis.service-key=test-kopis-key",
    "tour-api.service-key=test-tour-key"
})
@Import(TestContainerConfig.class)
class BatchJobExecutionRepositoryTest {

  @Autowired
  private BatchJobExecutionRepository batchJobExecutionRepository;
  @Autowired
  private JobRepository jobRepository;

  @Test
  @DisplayName("재시작으로 동일 Instance에 실행이 여러 개 생겨도 모두 최근순으로 반환한다")
  void returnsAllExecutionsForRestartedInstance() {
    String jobName = uniqueJobName();
    JobParameters parameters = new JobParametersBuilder()
        .addLong("triggeredAt", System.currentTimeMillis()).toJobParameters();
    JobInstance instance = jobRepository.createJobInstance(jobName, parameters);

    createExecution(instance, parameters, BatchStatus.FAILED, 5, 0, 0, 0, 0,
        LocalDateTime.now().minusMinutes(10));
    createExecution(instance, parameters, BatchStatus.COMPLETED, 10, 9, 2, 1, 1,
        LocalDateTime.now());

    Page<JobRunResult> page = batchJobExecutionRepository.findByJobName(
        jobName, PageRequest.of(0, 20));

    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent()).extracting(JobRunResult::status)
        .containsExactly("COMPLETED", "FAILED");
    // read/process/write skip을 각각 2/1/1로 설정 — 셋을 모두 합산해야 4가 나온다
    assertThat(page.getContent().get(0).skipCount()).isEqualTo(4);
  }

  @Test
  @DisplayName("페이지 크기를 넘는 실행 이력도 정확한 총 개수와 함께 페이징된다")
  void paginatesAcrossMultipleInstances() {
    String jobName = uniqueJobName();
    for (int i = 0; i < 3; i++) {
      JobParameters parameters = new JobParametersBuilder()
          .addLong("triggeredAt", System.currentTimeMillis() + i).toJobParameters();
      JobInstance instance = jobRepository.createJobInstance(jobName, parameters);
      createExecution(instance, parameters, BatchStatus.COMPLETED, 1, 1, 0, 0, 0,
          LocalDateTime.now().minusMinutes(3 - i));
    }

    Page<JobRunResult> firstPage = batchJobExecutionRepository.findByJobName(
        jobName, PageRequest.of(0, 2));
    Page<JobRunResult> secondPage = batchJobExecutionRepository.findByJobName(
        jobName, PageRequest.of(1, 2));

    assertThat(firstPage.getTotalElements()).isEqualTo(3);
    assertThat(firstPage.getContent()).hasSize(2);
    assertThat(secondPage.getContent()).hasSize(1);
  }

  @Test
  @DisplayName("실행 이력이 없는 Job은 빈 페이지를 반환한다")
  void returnsEmptyPageForJobWithoutHistory() {
    Page<JobRunResult> page = batchJobExecutionRepository.findByJobName(
        uniqueJobName(), PageRequest.of(0, 20));

    assertThat(page.getTotalElements()).isZero();
    assertThat(page.getContent()).isEmpty();
  }

  private void createExecution(
      JobInstance instance, JobParameters parameters, BatchStatus status,
      int read, int write, int readSkip, int processSkip, int writeSkip,
      LocalDateTime startTime) {
    JobExecution execution =
        jobRepository.createJobExecution(instance, parameters, new ExecutionContext());
    execution.setStatus(status);
    execution.setExitStatus(new ExitStatus(status.toString()));
    execution.setStartTime(startTime);
    execution.setEndTime(startTime.plusMinutes(1));
    jobRepository.update(execution);

    StepExecution stepExecution = jobRepository.createStepExecution("testStep", execution);
    stepExecution.setReadCount(read);
    stepExecution.setWriteCount(write);
    stepExecution.setReadSkipCount(readSkip);
    stepExecution.setProcessSkipCount(processSkip);
    stepExecution.setWriteSkipCount(writeSkip);
    jobRepository.update(stepExecution);
  }

  private String uniqueJobName() {
    return "testJob-" + UUID.randomUUID();
  }
}
