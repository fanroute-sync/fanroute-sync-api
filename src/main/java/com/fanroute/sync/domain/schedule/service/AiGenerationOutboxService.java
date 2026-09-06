package com.fanroute.sync.domain.schedule.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.schedule.config.AiGenerationStreamProperties;
import com.fanroute.sync.domain.schedule.entity.AiGenerationOutboxEvent;
import com.fanroute.sync.domain.schedule.entity.AiGenerationOutboxStatus;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.repository.AiGenerationOutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiGenerationOutboxService {

  private static final int DISPATCH_BATCH_SIZE = 20;

  private final AiGenerationOutboxEventRepository outboxRepository;
  private final AiGenerationStreamProperties properties;

  @Transactional
  public void enqueue(AiItineraryGeneration generation) {
    enqueueAt(generation, Instant.now());
  }

  @Transactional
  public void enqueueAt(AiItineraryGeneration generation, Instant nextAttemptAt) {
    outboxRepository.save(AiGenerationOutboxEvent.create(generation, nextAttemptAt));
  }

  @Transactional
  public List<Long> claimDispatchable() {
    Instant now = Instant.now();
    List<AiGenerationOutboxEvent> events = outboxRepository.lockDispatchable(now,
        AiGenerationOutboxStatus.PENDING, AiGenerationOutboxStatus.PUBLISHING,
        PageRequest.of(0, DISPATCH_BATCH_SIZE));
    Instant leaseUntil = now.plus(properties.getPublishLease());
    events.forEach(event -> event.claim(leaseUntil));
    return events.stream().map(AiGenerationOutboxEvent::getId).toList();
  }

  @Transactional
  public Long getGenerationId(Long eventId) {
    return outboxRepository.findById(eventId)
        .orElseThrow(() -> new IllegalStateException("Outbox event not found"))
        .getGeneration().getId();
  }

  @Transactional
  public void markPublished(Long eventId) {
    outboxRepository.findById(eventId).ifPresent(AiGenerationOutboxEvent::publish);
  }

  @Transactional
  public void reschedule(Long eventId) {
    outboxRepository.findById(eventId)
        .ifPresent(event -> event.reschedule(Instant.now().plus(properties.getOutboxPollDelay())));
  }
}
