package com.fanroute.sync.domain.place.config;

import java.time.Clock;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import com.fanroute.sync.domain.place.batch.PlaceItemProcessor;
import com.fanroute.sync.domain.place.batch.PlaceItemReader;
import com.fanroute.sync.domain.place.batch.PlaceItemWriter;
import com.fanroute.sync.domain.place.client.TourApiClient;
import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.repository.PlaceRepository;
import com.fanroute.sync.global.external.ExternalApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 카테고리 간 실패를 격리하기 위해 장소 동기화를 카테고리별 Job으로 구성합니다. */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PlaceSyncJobConfig {

  private static final int CHUNK_SIZE = 10;
  private static final int RETRY_LIMIT = 3;

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final TourApiClient tourApiClient;
  private final TourApiProperties tourApiProperties;
  private final PlaceRepository placeRepository;
  private final Clock clock;
  private final JobOperator jobOperator;

  @Bean
  public Job accommodationPlaceSyncJob() {
    return categoryJob(PlaceCategory.ACCOMMODATION);
  }

  @Bean
  public Job attractionPlaceSyncJob() {
    return categoryJob(PlaceCategory.ATTRACTION);
  }

  @Bean
  public Job restaurantPlaceSyncJob() {
    return categoryJob(PlaceCategory.RESTAURANT);
  }

  private Job categoryJob(PlaceCategory category) {
    return new JobBuilder(category.name().toLowerCase() + "PlaceSyncJob", jobRepository)
        .start(categoryStep(category))
        .build();
  }

  private Step categoryStep(PlaceCategory category) {
    return new StepBuilder(category.name().toLowerCase() + "PlaceSyncStep", jobRepository)
        .<TourApiDto.PlaceSummary, TourApiDto.PlaceSummary>chunk(CHUNK_SIZE)
        .reader(new PlaceItemReader(tourApiClient, tourApiProperties, category))
        .processor(new PlaceItemProcessor(category))
        .writer(new PlaceItemWriter(placeRepository, category, clock))
        .transactionManager(transactionManager)
        .faultTolerant()
        .retryLimit(RETRY_LIMIT)
        .retry(ExternalApiException.class)
        .build();
  }

  /** 한 카테고리의 실행 실패가 나머지 정기 동기화를 막지 않도록 격리합니다. */
  @Scheduled(cron = "${tour-api.sync-cron:0 0 5 * * *}")
  public void triggerPlaceSyncJobs() {
    for (PlaceCategory category : PlaceCategory.values()) {
      try {
        jobOperator.start(jobFor(category), triggerParameters());
      } catch (Exception exception) {
        log.error("TourAPI 장소 정기 동기화 실패: category={}", category, exception);
      }
    }
  }

  private JobParameters triggerParameters() {
    return new JobParametersBuilder()
        .addLong("triggeredAt", System.currentTimeMillis())
        .toJobParameters();
  }

  private Job jobFor(PlaceCategory category) {
    return switch (category) {
      case ACCOMMODATION -> accommodationPlaceSyncJob();
      case ATTRACTION -> attractionPlaceSyncJob();
      case RESTAURANT -> restaurantPlaceSyncJob();
    };
  }
}
