package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fanroute.sync.domain.schedule.client.GeminiDto;
import com.fanroute.sync.domain.schedule.client.GeminiRestClient;
import com.fanroute.sync.domain.schedule.config.AiGenerationRetryProperties;
import com.fanroute.sync.domain.schedule.config.AiGenerationNotificationProperties;
import com.fanroute.sync.domain.schedule.config.AiGenerationStreamProperties;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;
import com.fanroute.sync.domain.schedule.entity.AiGenerationNotificationType;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.repository.AiItineraryGenerationRepository;
import com.fanroute.sync.domain.schedule.repository.AiGenerationNotificationOutboxEventRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryItemRepository;
import com.fanroute.sync.domain.schedule.repository.TripPlanRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.support.AbstractRepositoryTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Testcontainers(disabledWithoutDocker = true)
@Import({
    AiGenerationNotificationOutboxService.class,
    AiGenerationNotificationProperties.class,
    AiGenerationOutboxService.class,
    AiGenerationRetryPolicy.class,
    AiGenerationRetryProperties.class,
    AiGenerationStreamProperties.class,
    AiItineraryGenerationAsyncService.class,
    AiItineraryGenerationService.class,
})
class AiGenerationRecoveryIntegrationTest extends AbstractRepositoryTest {

  private static final int REDIS_PORT = 6379;

  @Container
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(REDIS_PORT);

  private static LettuceConnectionFactory connectionFactory;
  private static StringRedisTemplate redisTemplate;

  @Autowired
  private AiItineraryGenerationAsyncService generationService;

  @Autowired
  private AiGenerationStreamProperties properties;

  @Autowired
  private AiItineraryGenerationRepository generationRepository;

  @Autowired
  private AiGenerationNotificationOutboxEventRepository notificationOutboxRepository;

  @Autowired
  private ItineraryItemRepository itineraryItemRepository;

  @Autowired
  private TripPlanRepository tripPlanRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @MockitoBean
  private GeminiRestClient geminiRestClient;

  @PersistenceContext
  private EntityManager entityManager;

  private boolean groupCreated;

  @BeforeAll
  static void setUpRedis() {
    connectionFactory = new LettuceConnectionFactory(
        REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT));
    connectionFactory.afterPropertiesSet();
    redisTemplate = new StringRedisTemplate(connectionFactory);
    redisTemplate.afterPropertiesSet();
  }

  @AfterAll
  static void closeRedisConnection() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
  }

  @BeforeEach
  void setUp() {
    redisTemplate.execute((RedisCallback<Void>) connection -> {
      connection.serverCommands().flushAll();
      return null;
    });
    properties.setKey("ai-generation-recovery-test");
    properties.setGroup("ai-generation-recovery-workers");
    properties.setConsumer("recovery-worker");
    properties.setProcessingLease(Duration.ofMillis(20));
    properties.setClaimMinIdle(Duration.ofMillis(20));
    groupCreated = false;
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DisplayName("pending 회수와 중복 전달에도 일정과 사용량을 한 번만 확정한다")
  void recoversPendingAndHandlesDuplicateGenerationOnce() throws Exception {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    GenerationFixture fixture = transactionTemplate.execute(status -> persistGeneration());
    when(geminiRestClient.generate(any())).thenReturn(new GeminiDto.GenerationResult(
        new GeminiDto.GeneratedItinerary(List.of(
            new GeminiDto.GeneratedItem("10:00", "부산 산책", 60, null))), null));
    AiGenerationStreamWorker worker = new AiGenerationStreamWorker(
        redisTemplate, properties, generationService);

    makePending(fixture.generationId());
    worker.recoverPending();

    assertThat(pendingCount()).isZero();
    GenerationState completed = transactionTemplate.execute(
        status -> findState(fixture.generationId(), fixture.tripPlanId(), fixture.dayId()));
    assertThat(completed).isEqualTo(new GenerationState(
        AiItineraryGenerationStatus.COMPLETED, 1, 0, 1));
    assertThat(notificationOutboxRepository.countByGenerationIdAndType(
        fixture.generationId(), AiGenerationNotificationType.COMPLETED)).isEqualTo(1);

    makePending(fixture.generationId());
    worker.recoverPending();

    assertThat(pendingCount()).isZero();
    GenerationState duplicate = transactionTemplate.execute(
        status -> findState(fixture.generationId(), fixture.tripPlanId(), fixture.dayId()));
    assertThat(duplicate).isEqualTo(completed);
    assertThat(notificationOutboxRepository.countByGenerationIdAndType(
        fixture.generationId(), AiGenerationNotificationType.COMPLETED)).isEqualTo(1);
    verify(geminiRestClient, times(1)).generate(any());
  }

  private GenerationFixture persistGeneration() {
    User user = User.create(
        "recovery-user", AuthProvider.GOOGLE, "recovery-provider-user");
    entityManager.persist(user);
    TripPlan tripPlan = TripPlan.create(user, null,
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T00:00:00Z"),
        null, List.of(), List.of());
    entityManager.persist(tripPlan);
    ItineraryDay day = ItineraryDay.create(tripPlan, LocalDate.of(2026, 9, 1), false);
    entityManager.persist(day);
    AiItineraryGeneration generation = AiItineraryGeneration.create(day);
    entityManager.persist(generation);
    entityManager.flush();
    assertThat(tripPlanRepository.reserveAiGeneration(
        tripPlan.getId(), TripPlan.AI_GENERATION_LIMIT)).isEqualTo(1);
    return new GenerationFixture(generation.getId(), tripPlan.getId(), day.getId());
  }

  @SuppressWarnings("unchecked")
  private void makePending(Long generationId) throws InterruptedException {
    redisTemplate.opsForStream().add(StreamRecords.newRecord()
        .ofMap(Map.of("generationId", generationId.toString()))
        .withStreamKey(properties.getKey()));
    if (!groupCreated) {
      redisTemplate.opsForStream().createGroup(properties.getKey(), ReadOffset.from("0-0"),
          properties.getGroup());
      groupCreated = true;
    }
    List<?> received = redisTemplate.opsForStream().read(
        Consumer.from(properties.getGroup(), "stopped-worker"),
        StreamReadOptions.empty().count(1),
        StreamOffset.create(properties.getKey(), ReadOffset.lastConsumed()));
    assertThat(received).hasSize(1);
    assertThat(pendingCount()).isEqualTo(1);
    Thread.sleep(50);
  }

  private long pendingCount() {
    return redisTemplate.opsForStream().pending(properties.getKey(), properties.getGroup(),
        Range.unbounded(), 10).size();
  }

  private GenerationState findState(Long generationId, Long tripPlanId, Long dayId) {
    AiItineraryGeneration generation = generationRepository.findById(generationId).orElseThrow();
    TripPlan tripPlan = tripPlanRepository.findById(tripPlanId).orElseThrow();
    int itemCount = itineraryItemRepository
        .findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(dayId).size();
    return new GenerationState(generation.getStatus(), tripPlan.getAiGenerationUsedCount(),
        tripPlan.getAiGenerationReservedCount(), itemCount);
  }

  private record GenerationFixture(Long generationId, Long tripPlanId, Long dayId) {
  }

  private record GenerationState(
      AiItineraryGenerationStatus status, int used, int reserved, int itemCount) {
  }
}
