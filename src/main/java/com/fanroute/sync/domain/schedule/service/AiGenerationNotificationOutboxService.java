package com.fanroute.sync.domain.schedule.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.schedule.config.AiGenerationNotificationProperties;
import com.fanroute.sync.domain.schedule.entity.AiGenerationNotificationOutboxEvent;
import com.fanroute.sync.domain.schedule.entity.AiGenerationNotificationStatus;
import com.fanroute.sync.domain.schedule.entity.AiGenerationNotificationType;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.repository.AiGenerationNotificationOutboxEventRepository;
import com.fanroute.sync.domain.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiGenerationNotificationOutboxService {

  private static final int MAX_FAILURE_REASON_LENGTH = 1000;

  private final AiGenerationNotificationOutboxEventRepository repository;
  private final AiGenerationNotificationProperties properties;

  public record Claim(Long eventId, int attemptCount) {
  }

  public record Delivery(Long eventId, Long generationId, Long userId,
                         AiGenerationNotificationType type) {
  }

  @Transactional
  public void enqueue(AiItineraryGeneration generation, AiGenerationNotificationType type) {
    User user = generation.getItineraryDay().getTripPlan().getUser();
    repository.save(AiGenerationNotificationOutboxEvent.create(
        generation, user, type, Instant.now()));
  }

  @Transactional
  public List<Claim> claimDispatchable() {
    Instant now = Instant.now();
    List<AiGenerationNotificationOutboxEvent> events = repository.lockDispatchable(
        now,
        AiGenerationNotificationStatus.PENDING,
        AiGenerationNotificationStatus.PROCESSING,
        PageRequest.of(0, properties.getBatchSize()));
    Instant leaseUntil = now.plus(properties.getProcessingLease());
    events.forEach(event -> event.claim(leaseUntil));
    return events.stream().map(event -> new Claim(event.getId(), event.getAttemptCount())).toList();
  }

  @Transactional(readOnly = true)
  public Delivery getDelivery(Claim claim) {
    AiGenerationNotificationOutboxEvent event = repository.findById(claim.eventId())
        .orElseThrow(() -> new IllegalStateException("AI notification event not found"));
    if (event.getStatus() != AiGenerationNotificationStatus.PROCESSING
        || event.getAttemptCount() != claim.attemptCount()) {
      return null;
    }
    return new Delivery(event.getId(), event.getGeneration().getId(), event.getUser().getId(),
        event.getType());
  }

  @Transactional
  public void markSent(Claim claim) {
    repository.findById(claim.eventId())
        .ifPresent(event -> event.sent(claim.attemptCount()));
  }

  @Transactional
  public void handleFailure(Claim claim, RuntimeException exception) {
    repository.findById(claim.eventId()).ifPresent(event -> {
      String failureReason = normalizeFailureReason(exception);
      if (event.getAttemptCount() >= properties.getMaxAttempts()) {
        event.fail(claim.attemptCount(), failureReason);
        return;
      }
      event.retry(claim.attemptCount(),
          Instant.now().plus(nextDelay(event.getAttemptCount())), failureReason);
    });
  }

  private Duration nextDelay(int attemptCount) {
    long initialMillis = properties.getInitialDelay().toMillis();
    int exponent = Math.max(0, Math.min(attemptCount - 1, 62));
    long exponentialMillis;
    try {
      exponentialMillis = Math.multiplyExact(initialMillis, 1L << exponent);
    } catch (ArithmeticException exception) {
      exponentialMillis = Long.MAX_VALUE;
    }
    double jitter = ThreadLocalRandom.current().nextDouble(-1, 1);
    long jitteredMillis = Math.max(1,
        Math.round(exponentialMillis * (1 + properties.getJitterRatio() * jitter)));
    return Duration.ofMillis(Math.min(jitteredMillis, properties.getMaxDelay().toMillis()));
  }

  private String normalizeFailureReason(RuntimeException exception) {
    String reason = exception.getClass().getSimpleName() + ": " + exception.getMessage();
    return reason.length() <= MAX_FAILURE_REASON_LENGTH
        ? reason : reason.substring(0, MAX_FAILURE_REASON_LENGTH);
  }
}
