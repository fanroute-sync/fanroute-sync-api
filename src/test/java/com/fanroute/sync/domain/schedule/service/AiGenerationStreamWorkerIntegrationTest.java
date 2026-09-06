package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fanroute.sync.domain.schedule.config.AiGenerationStreamProperties;

@Testcontainers(disabledWithoutDocker = true)
class AiGenerationStreamWorkerIntegrationTest {

  private static final int REDIS_PORT = 6379;

  @Container
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(REDIS_PORT);

  private static LettuceConnectionFactory connectionFactory;
  private static StringRedisTemplate redisTemplate;

  private AiGenerationStreamProperties properties;

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
    properties = new AiGenerationStreamProperties();
    properties.setKey("ai-generation-test");
    properties.setGroup("ai-workers-test");
    properties.setConsumer("recovery-worker");
    properties.setProcessingLease(Duration.ofMillis(20));
    properties.setClaimMinIdle(Duration.ofMillis(20));
  }

  @Test
  @DisplayName("종료된 consumer의 idle pending 메시지를 회수하고 ACK한다")
  @SuppressWarnings("unchecked")
  void reclaimsPendingMessageFromStoppedConsumer() throws Exception {
    redisTemplate.opsForStream().add(StreamRecords.newRecord()
        .ofMap(Map.of("generationId", "10"))
        .withStreamKey(properties.getKey()));
    redisTemplate.opsForStream().createGroup(properties.getKey(), ReadOffset.from("0-0"),
        properties.getGroup());
    List<?> received = redisTemplate.opsForStream().read(
        Consumer.from(properties.getGroup(), "stopped-worker"),
        StreamReadOptions.empty().count(1),
        StreamOffset.create(properties.getKey(), ReadOffset.lastConsumed()));
    assertThat(received).hasSize(1);
    assertThat(redisTemplate.opsForStream().pending(properties.getKey(), properties.getGroup(),
        Range.unbounded(), 10)).hasSize(1);
    Thread.sleep(50);

    AiItineraryGenerationAsyncService generationService =
        mock(AiItineraryGenerationAsyncService.class);
    when(generationService.generate(10L))
        .thenReturn(AiItineraryGenerationAsyncService.ExecutionResult.ACKNOWLEDGE);

    new AiGenerationStreamWorker(redisTemplate, properties, generationService).recoverPending();

    assertThat(redisTemplate.opsForStream().pending(properties.getKey(), properties.getGroup(),
        Range.unbounded(), 10)).isEmpty();
  }
}
