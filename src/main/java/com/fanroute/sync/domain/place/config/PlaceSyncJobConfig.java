package com.fanroute.sync.domain.place.config;

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

import com.fanroute.sync.domain.place.service.PlaceSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 기존 카테고리별 실패 격리 정책을 유지하기 위해 장소 동기화를 단일 Tasklet로 실행합니다. */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PlaceSyncJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final PlaceSyncService placeSyncService;
  private final JobOperator jobOperator;

  @Bean
  public Job tourApiPlaceSyncJob() {
    return new JobBuilder("tourApiPlaceSyncJob", jobRepository)
        .start(placeSyncStep())
        .build();
  }

  @Bean
  public Step placeSyncStep() {
    return new StepBuilder("placeSyncStep", jobRepository)
        .tasklet(placeSyncTasklet(), transactionManager)
        .build();
  }

  @Bean
  public Tasklet placeSyncTasklet() {
    return (contribution, chunkContext) -> {
      for (PlaceSyncService.SyncOutcome outcome : placeSyncService.syncAll()) {
        log.info(
            "TourAPI 장소 정기 동기화 완료: category={}, success={}, upsertedCount={}, "
                + "failureMessage={}",
            outcome.category(), outcome.success(), outcome.upsertedCount(),
            outcome.failureMessage());
      }
      return RepeatStatus.FINISHED;
    };
  }

  /** 실행 시각을 Job 파라미터로 사용해 스케줄 실행마다 새 Job Instance를 생성합니다. */
  @Scheduled(cron = "${tour-api.sync-cron:0 0 5 * * *}")
  public void triggerPlaceSyncJob() throws Exception {
    JobParameters jobParameters = new JobParametersBuilder()
        .addLong("triggeredAt", System.currentTimeMillis())
        .toJobParameters();
    jobOperator.start(tourApiPlaceSyncJob(), jobParameters);
  }
}
