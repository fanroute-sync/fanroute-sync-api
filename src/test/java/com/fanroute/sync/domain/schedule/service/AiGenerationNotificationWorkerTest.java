package com.fanroute.sync.domain.schedule.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fanroute.sync.domain.schedule.entity.AiGenerationNotificationType;

@ExtendWith(MockitoExtension.class)
class AiGenerationNotificationWorkerTest {

  @Mock
  private AiGenerationNotificationOutboxService outboxService;
  @Mock
  private AiGenerationNotificationSender sender;

  @Test
  @DisplayName("알림 발송 성공 뒤 event를 완료한다")
  void marksNotificationAsSent() {
    AiGenerationNotificationOutboxService.Claim claim = claim(1L);
    AiGenerationNotificationOutboxService.Delivery delivery = delivery(1L);
    when(outboxService.claimDispatchable()).thenReturn(List.of(claim));
    when(outboxService.getDelivery(claim)).thenReturn(delivery);

    worker().dispatch();

    verify(sender).send(delivery);
    verify(outboxService).markSent(claim);
  }

  @Test
  @DisplayName("한 알림의 실패를 격리하고 다음 알림을 계속 발송한다")
  void isolatesDeliveryFailure() {
    AiGenerationNotificationOutboxService.Claim firstClaim = claim(1L);
    AiGenerationNotificationOutboxService.Claim secondClaim = claim(2L);
    AiGenerationNotificationOutboxService.Delivery first = delivery(1L);
    AiGenerationNotificationOutboxService.Delivery second = delivery(2L);
    RuntimeException exception = new IllegalStateException("provider unavailable");
    when(outboxService.claimDispatchable()).thenReturn(List.of(firstClaim, secondClaim));
    when(outboxService.getDelivery(firstClaim)).thenReturn(first);
    when(outboxService.getDelivery(secondClaim)).thenReturn(second);
    doThrow(exception).when(sender).send(first);

    worker().dispatch();

    verify(outboxService).handleFailure(firstClaim, exception);
    verify(sender).send(second);
    verify(outboxService).markSent(secondClaim);
  }

  private AiGenerationNotificationWorker worker() {
    return new AiGenerationNotificationWorker(outboxService, sender);
  }

  private AiGenerationNotificationOutboxService.Claim claim(Long eventId) {
    return new AiGenerationNotificationOutboxService.Claim(eventId, 1);
  }

  private AiGenerationNotificationOutboxService.Delivery delivery(Long eventId) {
    return new AiGenerationNotificationOutboxService.Delivery(
        eventId, eventId + 10, eventId + 20, AiGenerationNotificationType.COMPLETED);
  }
}
