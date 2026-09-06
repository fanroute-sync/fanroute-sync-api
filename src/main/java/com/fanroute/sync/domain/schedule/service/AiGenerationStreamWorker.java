package com.fanroute.sync.domain.schedule.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.SmartLifecycle;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fanroute.sync.domain.schedule.config.AiGenerationStreamProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiGenerationStreamWorker implements SmartLifecycle {

  private static final long CLAIM_BATCH_SIZE = 10;

  private final StringRedisTemplate redisTemplate;
  private final AiGenerationStreamProperties properties;
  private final AiItineraryGenerationAsyncService generationService;
  private final AtomicBoolean running = new AtomicBoolean();
  private final String instanceSuffix = UUID.randomUUID().toString().substring(0, 8);

  private ExecutorService workerExecutor;

  @Override
  public synchronized void start() {
    if (!running.compareAndSet(false, true)) {
      return;
    }
    workerExecutor = Executors.newFixedThreadPool(properties.getWorkerConcurrency(),
        Thread.ofPlatform().name("ai-generation-worker-", 0).factory());
    for (int workerIndex = 0; workerIndex < properties.getWorkerConcurrency(); workerIndex++) {
      int index = workerIndex;
      workerExecutor.submit(() -> runWorker(index));
    }
  }

  @Override
  public synchronized void stop() {
    if (!running.compareAndSet(true, false) || workerExecutor == null) {
      return;
    }
    workerExecutor.shutdownNow();
    try {
      workerExecutor.awaitTermination(properties.getReadBlock().plusSeconds(1).toMillis(),
          TimeUnit.MILLISECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  void consume() {
    consume(consumerName(0));
  }

  void recoverPending() {
    recoverPending(consumerName(0));
  }

  String consumerName(int workerIndex) {
    return "%s-%s-%d".formatted(properties.getConsumer(), instanceSuffix, workerIndex);
  }

  private void runWorker(int workerIndex) {
    String consumerName = consumerName(workerIndex);
    boolean groupReady = false;
    Instant nextClaimAt = Instant.EPOCH;
    while (running.get() && !Thread.currentThread().isInterrupted()) {
      try {
        if (!groupReady) {
          groupReady = ensureConsumerGroup();
          if (!groupReady) {
            pauseAfterFailure();
            continue;
          }
        }
        Instant now = Instant.now();
        if (!now.isBefore(nextClaimAt)) {
          recoverPending(consumerName);
          nextClaimAt = now.plus(properties.getClaimPollDelay());
        }
        consume(consumerName);
      } catch (RuntimeException exception) {
        groupReady = false;
        log.warn("AI generation stream worker failed: consumer={}", consumerName, exception);
        pauseAfterFailure();
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void consume(String consumerName) {
    List<MapRecord<String, Object, Object>> records = streamOperations().read(
        Consumer.from(properties.getGroup(), consumerName),
        StreamReadOptions.empty().count(1).block(properties.getReadBlock()),
        StreamOffset.create(properties.getKey(), ReadOffset.lastConsumed()));
    if (records == null) {
      return;
    }
    for (MapRecord<String, Object, Object> record : records) {
      handle(record);
    }
  }

  private void recoverPending(String consumerName) {
    PendingMessages pending = streamOperations().pending(properties.getKey(),
        properties.getGroup(), Range.unbounded(), CLAIM_BATCH_SIZE, properties.getClaimMinIdle());
    if (pending.isEmpty()) {
      return;
    }
    RecordId[] recordIds = pending.stream()
        .map(message -> message.getId())
        .toArray(RecordId[]::new);
    List<MapRecord<String, Object, Object>> claimed = streamOperations().claim(
        properties.getKey(), properties.getGroup(), consumerName,
        properties.getClaimMinIdle(), recordIds);
    for (MapRecord<String, Object, Object> record : claimed) {
      handle(record);
    }
  }

  private void handle(MapRecord<String, Object, Object> record) {
    String generationId = String.valueOf(record.getValue().get("generationId"));
    try {
      AiItineraryGenerationAsyncService.ExecutionResult result = generationService.generate(
          Long.valueOf(generationId));
      if (result == AiItineraryGenerationAsyncService.ExecutionResult.ACKNOWLEDGE) {
        streamOperations().acknowledge(properties.getKey(), properties.getGroup(), record.getId());
      }
    } catch (RuntimeException exception) {
      log.warn("AI generation stream message handling failed: recordId={}", record.getId(),
          exception);
    }
  }

  private boolean ensureConsumerGroup() {
    try {
      streamOperations().createGroup(properties.getKey(), ReadOffset.from("0-0"),
          properties.getGroup());
      return true;
    } catch (DataAccessException exception) {
      String message = exception.getMostSpecificCause().getMessage();
      if (message != null && message.contains("BUSYGROUP")) {
        return true;
      }
      if (message != null && message.contains("no such key")) {
        return false;
      }
      throw exception;
    }
  }

  private void pauseAfterFailure() {
    try {
      Thread.sleep(properties.getConsumerPollDelay());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }

  private StreamOperations<String, Object, Object> streamOperations() {
    return redisTemplate.<Object, Object>opsForStream();
  }
}
