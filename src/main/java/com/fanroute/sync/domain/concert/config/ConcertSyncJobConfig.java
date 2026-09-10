package com.fanroute.sync.domain.concert.config;

import java.time.Clock;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import com.fanroute.sync.domain.concert.batch.ConcertItemProcessor;
import com.fanroute.sync.domain.concert.batch.ConcertItemReader;
import com.fanroute.sync.domain.concert.batch.ConcertItemWriter;
import com.fanroute.sync.domain.concert.batch.ConcertSyncDraft;
import com.fanroute.sync.domain.concert.client.KopisClient;
import com.fanroute.sync.domain.concert.dto.KopisDto;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.domain.concert.repository.ConcertScheduleRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.external.ExternalApiException;
import com.fanroute.sync.global.batch.BatchJobLauncher;

import lombok.RequiredArgsConstructor;

/** 공연별 응답 오류는 건너뛰고 전송 오류는 재시도하는 Chunk Job을 구성합니다. */
@Configuration
@RequiredArgsConstructor
public class ConcertSyncJobConfig {

  private static final int CHUNK_SIZE = 5;
  private static final int RETRY_LIMIT = 3;
  private static final int SKIP_LIMIT = 100;

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final KopisClient kopisClient;
  private final KopisProperties kopisProperties;
  private final VenueRepository venueRepository;
  private final ConcertRepository concertRepository;
  private final ConcertScheduleRepository concertScheduleRepository;
  private final Clock clock;
  private final BatchJobLauncher batchJobLauncher;

  @Bean
  public Job kopisConcertSyncJob() {
    return new JobBuilder("kopisConcertSyncJob", jobRepository)
        .start(concertSyncStep())
        .build();
  }

  @Bean
  public Step concertSyncStep() {
    return new StepBuilder("concertSyncStep", jobRepository)
        .<KopisDto.PerformanceSummary, ConcertSyncDraft>chunk(CHUNK_SIZE)
        .reader(new ConcertItemReader(kopisClient, kopisProperties, clock))
        .processor(new ConcertItemProcessor(kopisClient, kopisProperties))
        .writer(new ConcertItemWriter(
            venueRepository, concertRepository, concertScheduleRepository, clock))
        .transactionManager(transactionManager)
        .faultTolerant()
        .retryLimit(RETRY_LIMIT)
        .retry(ExternalApiException.class)
        .skipLimit(SKIP_LIMIT)
        .skip(ExternalApiException.class)
        .skip(BusinessException.class)
        .build();
  }

  /** 실행 시각을 파라미터로 사용해 스케줄마다 새 Job Instance를 생성합니다. */
  @Scheduled(cron = "${kopis.sync-cron:0 0 4 * * *}")
  public void triggerConcertSyncJob() throws Exception {
    JobParameters jobParameters = new JobParametersBuilder()
        .addLong("triggeredAt", System.currentTimeMillis())
        .toJobParameters();
    batchJobLauncher.start(kopisConcertSyncJob(), jobParameters);
  }
}
