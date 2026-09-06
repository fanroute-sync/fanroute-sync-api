package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import com.fanroute.sync.domain.schedule.config.AiGenerationStreamProperties;

@ExtendWith(MockitoExtension.class)
class AiGenerationStreamWorkerTest {

  @Mock
  private StringRedisTemplate redisTemplate;
  @Mock
  private StreamOperations<String, Object, Object> streamOperations;
  @Mock
  private AiItineraryGenerationAsyncService generationService;
  @Mock
  private MapRecord<String, Object, Object> record;

  private AiGenerationStreamProperties properties;

  @BeforeEach
  void setUp() {
    properties = new AiGenerationStreamProperties();
    properties.setKey("ai-stream");
    properties.setGroup("ai-workers");
    properties.setConsumer("worker-1");
  }

  @Test
  @DisplayName("idle 시간이 지난 pending 메시지를 claim하고 성공 후 ACK한다")
  void claimsIdlePendingAndAcknowledgesAfterProcessing() {
    when(redisTemplate.<Object, Object>opsForStream()).thenReturn(streamOperations);
    RecordId recordId = RecordId.of("1-0");
    PendingMessages pending = new PendingMessages(properties.getGroup(), List.of(
        new PendingMessage(recordId, Consumer.from(properties.getGroup(), "stopped-worker"),
            Duration.ofMinutes(2), 1)));
    when(streamOperations.pending(eq(properties.getKey()), eq(properties.getGroup()),
        any(Range.class), eq(10L), eq(properties.getClaimMinIdle()))).thenReturn(pending);
    when(streamOperations.claim(eq(properties.getKey()), eq(properties.getGroup()),
        any(String.class), eq(properties.getClaimMinIdle()), eq(recordId)))
        .thenReturn(List.of(record));
    when(record.getId()).thenReturn(recordId);
    when(record.getValue()).thenReturn(Map.of("generationId", "10"));
    when(generationService.generate(10L))
        .thenReturn(AiItineraryGenerationAsyncService.ExecutionResult.ACKNOWLEDGE);

    AiGenerationStreamWorker worker = worker();
    worker.recoverPending();

    verify(streamOperations).claim(eq(properties.getKey()), eq(properties.getGroup()),
        eq(worker.consumerName(0)), eq(properties.getClaimMinIdle()), eq(recordId));
    verify(streamOperations).acknowledge(properties.getKey(), properties.getGroup(), recordId);
  }

  @Test
  @DisplayName("processing lease가 유효한 회수 메시지는 ACK하지 않는다")
  void leavesActivelyProcessedMessagePending() {
    when(redisTemplate.<Object, Object>opsForStream()).thenReturn(streamOperations);
    RecordId recordId = RecordId.of("1-0");
    PendingMessages pending = new PendingMessages(properties.getGroup(), List.of(
        new PendingMessage(recordId, Consumer.from(properties.getGroup(), "stopped-worker"),
            Duration.ofMinutes(2), 1)));
    when(streamOperations.pending(eq(properties.getKey()), eq(properties.getGroup()),
        any(Range.class), eq(10L), eq(properties.getClaimMinIdle()))).thenReturn(pending);
    when(streamOperations.claim(eq(properties.getKey()), eq(properties.getGroup()),
        any(String.class), eq(properties.getClaimMinIdle()), eq(recordId)))
        .thenReturn(List.of(record));
    when(record.getValue()).thenReturn(Map.of("generationId", "10"));
    when(generationService.generate(10L))
        .thenReturn(AiItineraryGenerationAsyncService.ExecutionResult.LEAVE_PENDING);

    worker().recoverPending();

    verify(streamOperations, never()).acknowledge(any(), any(), any(RecordId[].class));
  }

  @Test
  @DisplayName("같은 instance의 worker마다 고유 consumer 이름을 사용한다")
  void createsUniqueConsumerNamePerWorker() {
    AiGenerationStreamWorker worker = worker();

    assertThat(worker.consumerName(0)).startsWith("worker-1-").endsWith("-0");
    assertThat(worker.consumerName(1)).startsWith("worker-1-").endsWith("-1");
    assertThat(worker.consumerName(0)).isNotEqualTo(worker.consumerName(1));
  }

  private AiGenerationStreamWorker worker() {
    return new AiGenerationStreamWorker(redisTemplate, properties, generationService);
  }
}
