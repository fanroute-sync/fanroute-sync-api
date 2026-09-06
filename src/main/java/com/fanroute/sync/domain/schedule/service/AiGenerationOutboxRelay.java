package com.fanroute.sync.domain.schedule.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fanroute.sync.domain.schedule.config.AiGenerationStreamProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiGenerationOutboxRelay {

  private final AiGenerationOutboxService outboxService;
  private final StringRedisTemplate redisTemplate;
  private final AiGenerationStreamProperties properties;

  @Scheduled(fixedDelayString = "${ai-generation.stream.outbox-poll-delay:1s}")
  public void relay() {
    List<Long> eventIds = outboxService.claimDispatchable();
    for (Long eventId : eventIds) {
      try {
        Long generationId = outboxService.getGenerationId(eventId);
        redisTemplate.opsForStream().add(StreamRecords.newRecord()
            .ofMap(Map.of("generationId", generationId.toString()))
            .withStreamKey(properties.getKey()));
        outboxService.markPublished(eventId);
      } catch (RuntimeException exception) {
        log.warn("AI generation outbox relay failed: eventId={}", eventId,
            exception);
        outboxService.reschedule(eventId);
      }
    }
  }
}
