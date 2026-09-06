package com.fanroute.sync.domain.schedule.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(AiGenerationNotificationSender.class)
@ConditionalOnProperty(prefix = "ai-generation.notification", name = "enabled",
    havingValue = "true")
public class AiGenerationNotificationWorker {

  private final AiGenerationNotificationOutboxService outboxService;
  private final AiGenerationNotificationSender sender;

  @Scheduled(fixedDelayString = "${ai-generation.notification.poll-delay:1s}")
  public void dispatch() {
    List<AiGenerationNotificationOutboxService.Claim> claims = outboxService.claimDispatchable();
    for (AiGenerationNotificationOutboxService.Claim claim : claims) {
      try {
        AiGenerationNotificationOutboxService.Delivery delivery = outboxService.getDelivery(claim);
        if (delivery == null) {
          continue;
        }
        sender.send(delivery);
        outboxService.markSent(claim);
      } catch (RuntimeException exception) {
        log.warn("AI generation notification delivery failed: eventId={}", claim.eventId(),
            exception);
        outboxService.handleFailure(claim, exception);
      }
    }
  }
}
