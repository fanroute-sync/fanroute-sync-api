package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fanroute.sync.domain.schedule.config.AiGenerationNotificationProperties;
import com.fanroute.sync.domain.schedule.entity.AiGenerationNotificationOutboxEvent;
import com.fanroute.sync.domain.schedule.entity.AiGenerationNotificationType;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.repository.AiGenerationNotificationOutboxEventRepository;
import com.fanroute.sync.domain.user.entity.User;

@ExtendWith(MockitoExtension.class)
class AiGenerationNotificationOutboxServiceTest {

  @Mock
  private AiGenerationNotificationOutboxEventRepository repository;
  @Mock
  private AiGenerationNotificationOutboxEvent event;

  private AiGenerationNotificationProperties properties;
  private AiGenerationNotificationOutboxService service;

  @BeforeEach
  void setUp() {
    properties = new AiGenerationNotificationProperties();
    properties.setProcessingLease(Duration.ofSeconds(30));
    properties.setInitialDelay(Duration.ofSeconds(2));
    properties.setMaxDelay(Duration.ofSeconds(10));
    properties.setJitterRatio(0);
    service = new AiGenerationNotificationOutboxService(repository, properties);
  }

  @Test
  @DisplayName("완료 상태와 사용자를 notification outbox에 기록한다")
  void enqueuesNotification() {
    AiItineraryGeneration generation = org.mockito.Mockito.mock(AiItineraryGeneration.class);
    ItineraryDay day = org.mockito.Mockito.mock(ItineraryDay.class);
    TripPlan tripPlan = org.mockito.Mockito.mock(TripPlan.class);
    User user = org.mockito.Mockito.mock(User.class);
    when(generation.getItineraryDay()).thenReturn(day);
    when(day.getTripPlan()).thenReturn(tripPlan);
    when(tripPlan.getUser()).thenReturn(user);

    service.enqueue(generation, AiGenerationNotificationType.COMPLETED);

    ArgumentCaptor<AiGenerationNotificationOutboxEvent> captor =
        ArgumentCaptor.forClass(AiGenerationNotificationOutboxEvent.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getGeneration()).isSameAs(generation);
    assertThat(captor.getValue().getUser()).isSameAs(user);
    assertThat(captor.getValue().getType()).isEqualTo(AiGenerationNotificationType.COMPLETED);
  }

  @Test
  @DisplayName("발송 가능한 event를 lease로 선점한다")
  void claimsDispatchableEvents() {
    when(repository.lockDispatchable(any(), any(), any(), any())).thenReturn(List.of(event));
    when(event.getId()).thenReturn(1L);
    when(event.getAttemptCount()).thenReturn(1);

    assertThat(service.claimDispatchable()).containsExactly(
        new AiGenerationNotificationOutboxService.Claim(1L, 1));

    verify(event).claim(any(Instant.class));
  }

  @Test
  @DisplayName("발송 실패는 지수 backoff 뒤 재시도 상태로 되돌린다")
  void schedulesRetryAfterFailure() {
    when(repository.findById(1L)).thenReturn(java.util.Optional.of(event));
    when(event.getAttemptCount()).thenReturn(1);
    AiGenerationNotificationOutboxService.Claim claim =
        new AiGenerationNotificationOutboxService.Claim(1L, 1);

    Instant before = Instant.now();
    service.handleFailure(claim, new IllegalStateException("provider unavailable"));

    ArgumentCaptor<Instant> nextAttemptCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(event).retry(org.mockito.ArgumentMatchers.eq(1), nextAttemptCaptor.capture(),
        org.mockito.ArgumentMatchers.contains("provider unavailable"));
    assertThat(nextAttemptCaptor.getValue()).isBetween(
        before.plusSeconds(2), Instant.now().plusSeconds(2));
    verify(event, never()).fail(any(Integer.class), any());
  }

  @Test
  @DisplayName("최대 발송 횟수에 도달하면 event만 최종 실패시킨다")
  void failsAfterMaximumAttempts() {
    properties.setMaxAttempts(3);
    when(repository.findById(1L)).thenReturn(java.util.Optional.of(event));
    when(event.getAttemptCount()).thenReturn(3);
    AiGenerationNotificationOutboxService.Claim claim =
        new AiGenerationNotificationOutboxService.Claim(1L, 3);

    service.handleFailure(claim, new IllegalStateException("provider unavailable"));

    verify(event).fail(org.mockito.ArgumentMatchers.eq(3),
        org.mockito.ArgumentMatchers.contains("provider unavailable"));
    verify(event, never()).retry(any(Integer.class), any(), any());
  }
}
