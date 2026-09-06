package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fanroute.sync.domain.schedule.config.AiGenerationStreamProperties;
import com.fanroute.sync.domain.schedule.entity.AiGenerationOutboxEvent;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.repository.AiGenerationOutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class AiGenerationOutboxServiceTest {

  @Mock
  private AiGenerationOutboxEventRepository outboxRepository;
  @Mock
  private AiItineraryGeneration generation;

  @Test
  @DisplayName("재시도 outbox는 지정된 다음 시도 시각을 저장한다")
  void enqueuesAtScheduledTime() {
    Instant nextAttemptAt = Instant.parse("2026-09-01T00:01:00Z");
    when(outboxRepository.save(any(AiGenerationOutboxEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service().enqueueAt(generation, nextAttemptAt);

    ArgumentCaptor<AiGenerationOutboxEvent> eventCaptor = ArgumentCaptor.forClass(
        AiGenerationOutboxEvent.class);
    verify(outboxRepository).save(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getGeneration()).isSameAs(generation);
    assertThat(eventCaptor.getValue().getNextAttemptAt()).isEqualTo(nextAttemptAt);
  }

  private AiGenerationOutboxService service() {
    return new AiGenerationOutboxService(outboxRepository, new AiGenerationStreamProperties());
  }
}
