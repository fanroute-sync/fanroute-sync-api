package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
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
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fanroute.sync.domain.schedule.client.GeminiRestClient;
import com.fanroute.sync.domain.schedule.config.AiGenerationRetryProperties;
import com.fanroute.sync.domain.schedule.config.AiGenerationNotificationProperties;
import com.fanroute.sync.domain.schedule.config.AiGenerationStreamProperties;
import com.fanroute.sync.domain.schedule.entity.AiGenerationDeadLetter;
import com.fanroute.sync.domain.schedule.entity.AiGenerationNotificationType;
import com.fanroute.sync.domain.schedule.entity.AiGenerationOutboxStatus;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.repository.AiGenerationDeadLetterRepository;
import com.fanroute.sync.domain.schedule.repository.AiGenerationNotificationOutboxEventRepository;
import com.fanroute.sync.domain.schedule.repository.AiGenerationOutboxEventRepository;
import com.fanroute.sync.domain.schedule.repository.AiItineraryGenerationRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.repository.UserRepository;
import com.fanroute.sync.global.external.ExternalApiErrorType;
import com.fanroute.sync.global.external.ExternalApiException;
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
    AiPlaceCandidateRanker.class,
})
class AiGenerationRetryIntegrationTest extends AbstractRepositoryTest {

  private static final int REDIS_PORT = 6379;
  private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

  @Container
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(REDIS_PORT);

  private static LettuceConnectionFactory connectionFactory;
  private static StringRedisTemplate redisTemplate;

  @Autowired
  private AiItineraryGenerationAsyncService generationService;

  @Autowired
  private AiGenerationOutboxService outboxService;

  @Autowired
  private AiGenerationStreamProperties streamProperties;

  @Autowired
  private AiGenerationRetryProperties retryProperties;

  @Autowired
  private AiItineraryGenerationRepository generationRepository;

  @Autowired
  private AiGenerationOutboxEventRepository outboxRepository;

  @Autowired
  private AiGenerationDeadLetterRepository deadLetterRepository;

  @Autowired
  private AiGenerationNotificationOutboxEventRepository notificationOutboxRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @MockitoBean
  private GeminiRestClient geminiRestClient;

  @PersistenceContext
  private EntityManager entityManager;

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
    streamProperties.setKey("ai-generation-retry-test");
    streamProperties.setGroup("ai-generation-retry-workers");
    streamProperties.setConsumer("retry-worker");
    streamProperties.setOutboxPollDelay(Duration.ZERO);
    retryProperties.setMaxAttempts(3);
    retryProperties.setInitialDelay(RETRY_DELAY);
    retryProperties.setMaxDelay(RETRY_DELAY);
    retryProperties.setJitterRatio(0);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DisplayName("429·503·network timeout을 지연 재시도하고 최대 시도 뒤 DLQ 처리한다")
  void retriesTransientFailuresAndDeadLettersAfterMaxAttempts() throws Exception {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    GenerationFixture fixture = transactionTemplate.execute(status -> persistGeneration());
    when(geminiRestClient.generate(any()))
        .thenThrow(ExternalApiException.responseError(
            ExternalApiErrorType.CLIENT_ERROR, HttpStatus.TOO_MANY_REQUESTS, "rate limited"))
        .thenThrow(ExternalApiException.responseError(
            ExternalApiErrorType.SERVER_ERROR, HttpStatus.SERVICE_UNAVAILABLE,
            "provider unavailable"))
        .thenThrow(ExternalApiException.transportError(
            ExternalApiErrorType.READ_TIMEOUT, "read timed out", new SocketTimeoutException()));
    AiGenerationStreamWorker worker = new AiGenerationStreamWorker(
        redisTemplate, streamProperties, generationService);
    AiGenerationOutboxRelay relay = new AiGenerationOutboxRelay(
        outboxService, redisTemplate, streamProperties);
    publishInitialMessage(fixture.generationId());

    worker.consume();
    RetryState firstRetry = transactionTemplate.execute(
        status -> findRetryState(fixture.generationId(), fixture.userId()));
    assertRetryScheduled(firstRetry, 1, "rate limited");
    assertThat(notificationOutboxRepository.countByGenerationId(fixture.generationId())).isZero();
    assertThat(streamSize()).isEqualTo(1);
    relay.relay();
    assertThat(streamSize()).isEqualTo(1);

    waitUntil(firstRetry.nextAttemptAt());
    relay.relay();
    assertThat(streamSize()).isEqualTo(2);
    worker.consume();
    RetryState secondRetry = transactionTemplate.execute(
        status -> findRetryState(fixture.generationId(), fixture.userId()));
    assertRetryScheduled(secondRetry, 2, "provider unavailable");
    assertThat(notificationOutboxRepository.countByGenerationId(fixture.generationId())).isZero();
    relay.relay();
    assertThat(streamSize()).isEqualTo(2);

    waitUntil(secondRetry.nextAttemptAt());
    relay.relay();
    assertThat(streamSize()).isEqualTo(3);
    worker.consume();

    FinalState finalState = transactionTemplate.execute(
        status -> findFinalState(fixture.generationId(), fixture.userId()));
    assertThat(finalState).isEqualTo(new FinalState(
        AiItineraryGenerationStatus.FAILED, 3, 0, 0, 1, 2));
    assertThat(pendingCount()).isZero();
    assertThat(notificationOutboxRepository.countByGenerationIdAndType(
        fixture.generationId(), AiGenerationNotificationType.FAILED)).isEqualTo(1);
    verify(geminiRestClient, times(3)).generate(any());
  }

  private GenerationFixture persistGeneration() {
    User user = User.create(
        "retry-user", AuthProvider.GOOGLE, "retry-provider-user");
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
    assertThat(userRepository.reserveAiGeneration(
        user.getId(), User.AI_GENERATION_LIMIT)).isEqualTo(1);
    return new GenerationFixture(generation.getId(), user.getId());
  }

  private void publishInitialMessage(Long generationId) {
    redisTemplate.opsForStream().add(StreamRecords.newRecord()
        .ofMap(Map.of("generationId", generationId.toString()))
        .withStreamKey(streamProperties.getKey()));
    redisTemplate.opsForStream().createGroup(streamProperties.getKey(), ReadOffset.from("0-0"),
        streamProperties.getGroup());
  }

  private void assertRetryScheduled(RetryState state, int attemptCount, String failureMessage) {
    assertThat(state.status()).isEqualTo(AiItineraryGenerationStatus.PENDING);
    assertThat(state.attemptCount()).isEqualTo(attemptCount);
    assertThat(state.nextAttemptAt()).isAfter(Instant.now());
    assertThat(state.lastFailureReason()).contains(failureMessage);
    assertThat(state.used()).isZero();
    assertThat(state.reserved()).isEqualTo(1);
    assertThat(pendingCount()).isZero();
  }

  private RetryState findRetryState(Long generationId, Long userId) {
    AiItineraryGeneration generation = generationRepository.findById(generationId).orElseThrow();
    User user = userRepository.findById(userId).orElseThrow();
    return new RetryState(generation.getStatus(), generation.getAttemptCount(),
        generation.getNextAttemptAt(), generation.getLastFailureReason(),
        user.getAiGenerationUsedCount(), user.getAiGenerationReservedCount());
  }

  private FinalState findFinalState(Long generationId, Long userId) {
    AiItineraryGeneration generation = generationRepository.findById(generationId).orElseThrow();
    User user = userRepository.findById(userId).orElseThrow();
    long deadLetterCount = deadLetterRepository.findAll().stream()
        .map(AiGenerationDeadLetter::getGeneration)
        .filter(deadLetterGeneration -> deadLetterGeneration.getId().equals(generationId))
        .count();
    long publishedOutboxCount = outboxRepository.findAll().stream()
        .filter(event -> event.getGeneration().getId().equals(generationId))
        .filter(event -> event.getStatus() == AiGenerationOutboxStatus.PUBLISHED)
        .count();
    return new FinalState(generation.getStatus(), generation.getAttemptCount(),
        user.getAiGenerationUsedCount(), user.getAiGenerationReservedCount(),
        deadLetterCount, publishedOutboxCount);
  }

  private void waitUntil(Instant nextAttemptAt) throws InterruptedException {
    long waitMillis = Math.max(1,
        Duration.between(Instant.now(), nextAttemptAt).toMillis() + 50);
    Thread.sleep(waitMillis);
  }

  private long streamSize() {
    Long size = redisTemplate.opsForStream().size(streamProperties.getKey());
    return size == null ? 0 : size;
  }

  private long pendingCount() {
    return redisTemplate.opsForStream().pending(streamProperties.getKey(),
        streamProperties.getGroup(), Range.unbounded(), 10).size();
  }

  private record GenerationFixture(Long generationId, Long userId) {
  }

  private record RetryState(
      AiItineraryGenerationStatus status,
      int attemptCount,
      Instant nextAttemptAt,
      String lastFailureReason,
      int used,
      int reserved) {
  }

  private record FinalState(
      AiItineraryGenerationStatus status,
      int attemptCount,
      int used,
      int reserved,
      long deadLetterCount,
      long publishedOutboxCount) {
  }
}
