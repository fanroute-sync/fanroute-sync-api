package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fanroute.sync.domain.schedule.config.AiGenerationStreamProperties;
import com.fanroute.sync.domain.schedule.entity.AiGenerationOutboxEvent;
import com.fanroute.sync.domain.schedule.entity.AiGenerationOutboxStatus;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.repository.AiGenerationOutboxEventRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.support.AbstractRepositoryTest;
import com.fanroute.sync.support.UserFixture;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Import({
    AiGenerationOutboxRelay.class,
    AiGenerationOutboxService.class,
    AiGenerationStreamProperties.class,
})
class AiGenerationOutboxRelayIntegrationTest extends AbstractRepositoryTest {

  @Autowired
  private AiGenerationOutboxRelay relay;

  @Autowired
  private AiGenerationStreamProperties properties;

  @Autowired
  private AiGenerationOutboxEventRepository outboxRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @MockitoBean
  private StringRedisTemplate redisTemplate;

  @PersistenceContext
  private EntityManager entityManager;

  private StreamOperations<String, Object, Object> streamOperations;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    streamOperations = mock(StreamOperations.class);
    when(redisTemplate.<Object, Object>opsForStream()).thenReturn(streamOperations);
    properties.setOutboxPollDelay(Duration.ZERO);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DisplayName("Redis 발행 실패 뒤 outbox를 남기고 다음 relay에서 복구한다")
  void recoversOutboxAfterRedisPublishFailure() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    Long eventId = transactionTemplate.execute(status -> persistOutboxEvent());
    when(streamOperations.add(any()))
        .thenThrow(new RedisConnectionFailureException("Redis unavailable"))
        .thenReturn(RecordId.of("1-0"));

    relay.relay();

    OutboxState failedState = transactionTemplate.execute(status -> findState(eventId));
    assertThat(failedState.status()).isEqualTo(AiGenerationOutboxStatus.PENDING);
    assertThat(failedState.publishLeaseUntil()).isNull();

    relay.relay();

    OutboxState recoveredState = transactionTemplate.execute(status -> findState(eventId));
    assertThat(recoveredState.status()).isEqualTo(AiGenerationOutboxStatus.PUBLISHED);
    assertThat(recoveredState.publishLeaseUntil()).isNull();
    verify(streamOperations, times(2)).add(any());
  }

  private Long persistOutboxEvent() {
    User user = UserFixture.activeUser();
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
    return event.getId();
  }

  private OutboxState findState(Long eventId) {
    return outboxRepository.findById(eventId)
        .map(event -> new OutboxState(event.getStatus(), event.getPublishLeaseUntil()))
        .orElseThrow();
  }

  private record OutboxState(AiGenerationOutboxStatus status, Instant publishLeaseUntil) {
  }
}
