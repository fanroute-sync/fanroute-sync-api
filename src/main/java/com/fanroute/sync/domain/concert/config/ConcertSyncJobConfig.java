package com.fanroute.sync.domain.concert.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import com.fanroute.sync.domain.concert.service.ConcertSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 기존 건별 실패 처리 정책을 유지하기 위해 공연 동기화를 단일 Tasklet로 실행합니다. */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ConcertSyncJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final ConcertSyncService concertSyncService;
  private final JobOperator jobOperator;

  @Bean
  public Job kopisConcertSyncJob() {
    return new JobBuilder("kopisConcertSyncJob", jobRepository)
        .start(concertSyncStep())
        .build();
  }

  @Bean
  public Step concertSyncStep() {
    return new StepBuilder("concertSyncStep", jobRepository)
        .tasklet(concertSyncTasklet(), transactionManager)
        .build();
  }

  @Bean
  public Tasklet concertSyncTasklet() {
    return (contribution, chunkContext) -> {
      ConcertSyncService.SyncResult result = concertSyncService.syncConcerts();
      log.info("KOPIS 공연 정기 동기화 완료: total={}, succeeded={}, failed={}",
          result.total(), result.succeeded(), result.failed());
      return RepeatStatus.FINISHED;
    };
  }

  /** 실행 시각을 Job 파라미터로 사용해 스케줄 실행마다 새 Job Instance를 생성합니다. */
  @Scheduled(cron = "${kopis.sync-cron:0 0 4 * * *}")
  public void triggerConcertSyncJob() throws Exception {
    JobParameters jobParameters = new JobParametersBuilder()
        .addLong("triggeredAt", System.currentTimeMillis())
        .toJobParameters();
    jobOperator.start(kopisConcertSyncJob(), jobParameters);
  }
}
