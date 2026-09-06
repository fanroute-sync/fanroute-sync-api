package com.fanroute.sync.domain.schedule.service;

public interface AiGenerationNotificationSender {

  void send(AiGenerationNotificationOutboxService.Delivery delivery);
}
