package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fanroute.sync.domain.schedule.config.AiGenerationStreamProperties;
import com.fanroute.sync.domain.schedule.entity.AiGenerationOutboxEvent;
import com.fanroute.sync.domain.schedule.entity.AiGenerationOutboxStatus;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.repository.AiGenerationOutboxEventRepository;
import com.fanroute.sync.domain.schedule.repository.AiItineraryGenerationRepository;
import com.fanroute.sync.domain.schedule.repository.TripPlanRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.support.AbstractRepositoryTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Testcontainers(disabledWithoutDocker = true)
@Import({AiGenerationOutboxService.class, AiGenerationStreamProperties.class})
class AiGenerationRedisOutageIntegrationTest extends AbstractRepositoryTest {

  private static final int REDIS_PORT = 6379;
  private static final Duration REDIS_COMMAND_TIMEOUT = Duration.ofMillis(300);

  @Container
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(REDIS_PORT);

  private static LettuceConnectionFactory connectionFactory;
  private static StringRedisTemplate redisTemplate;

  @Autowired
  private AiGenerationOutboxService outboxService;

  @Autowired
  private AiGenerationStreamProperties streamProperties;

  @Autowired
  private AiGenerationOutboxEventRepository outboxRepository;

  @Autowired
  private AiItineraryGenerationRepository generationRepository;

  @Autowired
  private TripPlanRepository tripPlanRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @PersistenceContext
  private EntityManager entityManager;

  @BeforeAll
  static void setUpRedis() {
    RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration(
        REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT));
    LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
        .commandTimeout(REDIS_COMMAND_TIMEOUT)
        .shutdownTimeout(Duration.ZERO)
        .build();
    connectionFactory = new LettuceConnectionFactory(redisConfiguration, clientConfiguration);
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
    streamProperties.setKey("ai-generation-redis-outage-test");
    streamProperties.setOutboxPollDelay(Duration.ZERO);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DisplayName("Redis 일시 중단에도 DB 원장을 유지하고 복구 뒤 outbox를 발행한다")
  void republishesOutboxAfterRedisRecovers() throws Exception {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    Long eventId = transactionTemplate.execute(status -> persistOutboxEvent());
    AiGenerationOutboxRelay relay = new AiGenerationOutboxRelay(
        outboxService, redisTemplate, streamProperties);
    boolean paused = false;

    try {
      REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
      paused = true;
      relay.relay();
    } finally {
      if (paused) {
        REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
      }
    }

    OutageState outageState = transactionTemplate.execute(status -> findState(eventId));
    assertThat(outageState).isEqualTo(new OutageState(
        AiGenerationOutboxStatus.PENDING,
        AiItineraryGenerationStatus.PENDING,
        0,
        1));

    awaitRedisRecovery();
    relay.relay();

    OutageState recoveredState = transactionTemplate.execute(status -> findState(eventId));
    assertThat(recoveredState.outboxStatus()).isEqualTo(AiGenerationOutboxStatus.PUBLISHED);
    assertThat(recoveredState.generationStatus()).isEqualTo(AiItineraryGenerationStatus.PENDING);
    assertThat(recoveredState.used()).isZero();
    assertThat(recoveredState.reserved()).isEqualTo(1);
    List<MapRecord<String, Object, Object>> records = redisTemplate
        .<Object, Object>opsForStream()
        .range(streamProperties.getKey(), org.springframework.data.domain.Range.unbounded());
    Long generationId = eventGenerationId(eventId);
    assertThat(records).hasSizeBetween(1, 2)
        .allSatisfy(record -> assertThat(record.getValue().get("generationId"))
            .isEqualTo(generationId.toString()));
  }

  private Long persistOutboxEvent() {
    User user = User.create("redis-outage-user", AuthProvider.GOOGLE,
        "redis-outage-provider-user");
    entityManager.persist(user);
    TripPlan tripPlan = TripPlan.create(user, null,
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T00:00:00Z"),
        null, List.of(), List.of());
    entityManager.persist(tripPlan);
    ItineraryDay day = ItineraryDay.create(tripPlan, LocalDate.of(2026, 9, 1), false);
    entityManager.persist(day);
    AiItineraryGeneration generation = AiItineraryGeneration.create(day);
    entityManager.persist(generation);
    AiGenerationOutboxEvent event = AiGenerationOutboxEvent.create(generation, Instant.now());
    entityManager.persist(event);
    entityManager.flush();
    assertThat(tripPlanRepository.reserveAiGeneration(
        tripPlan.getId(), TripPlan.AI_GENERATION_LIMIT)).isEqualTo(1);
    return event.getId();
  }

  private OutageState findState(Long eventId) {
    AiGenerationOutboxEvent event = outboxRepository.findById(eventId).orElseThrow();
    AiItineraryGeneration generation = generationRepository
        .findById(event.getGeneration().getId()).orElseThrow();
    TripPlan tripPlan = tripPlanRepository
        .findById(generation.getItineraryDay().getTripPlan().getId()).orElseThrow();
    return new OutageState(event.getStatus(), generation.getStatus(),
        tripPlan.getAiGenerationUsedCount(), tripPlan.getAiGenerationReservedCount());
  }

  private Long eventGenerationId(Long eventId) {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    return transactionTemplate.execute(status -> outboxRepository.findById(eventId)
        .map(event -> event.getGeneration().getId())
        .orElseThrow());
  }

  private void awaitRedisRecovery() throws InterruptedException {
    Instant deadline = Instant.now().plusSeconds(5);
    while (Instant.now().isBefore(deadline)) {
      try {
        redisTemplate.hasKey(streamProperties.getKey());
        return;
      } catch (RedisConnectionFailureException exception) {
        Thread.sleep(100);
      }
    }
    throw new IllegalStateException("Redis connection did not recover within timeout");
  }

  private record OutageState(
      AiGenerationOutboxStatus outboxStatus,
      AiItineraryGenerationStatus generationStatus,
      int used,
      int reserved) {
  }
}
