package com.fanroute.sync.domain.schedule.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fanroute.sync.domain.user.entity.User;

class AiGenerationNotificationOutboxEventTest {

  @Test
  @DisplayName("lease 재선점 뒤 이전 worker의 완료는 상태를 덮어쓰지 않는다")
  void rejectsStaleClaimCompletion() {
    AiGenerationNotificationOutboxEvent event = AiGenerationNotificationOutboxEvent.create(
        org.mockito.Mockito.mock(AiItineraryGeneration.class),
        org.mockito.Mockito.mock(User.class),
        AiGenerationNotificationType.COMPLETED,
        Instant.now());
    event.claim(Instant.now().plusSeconds(30));
    event.claim(Instant.now().plusSeconds(60));

    assertThat(event.sent(1)).isFalse();
    assertThat(event.getStatus()).isEqualTo(AiGenerationNotificationStatus.PROCESSING);
    assertThat(event.sent(2)).isTrue();
    assertThat(event.getStatus()).isEqualTo(AiGenerationNotificationStatus.SENT);
  }
}
