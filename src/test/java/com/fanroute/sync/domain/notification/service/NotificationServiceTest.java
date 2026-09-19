package com.fanroute.sync.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.notification.entity.Notification;
import com.fanroute.sync.domain.notification.repository.NotificationRepository;
import com.fanroute.sync.domain.notification.repository.PushTokenRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
  @Mock NotificationRepository notifications;
  @Mock PushTokenRepository tokens;
  @Mock FirebasePushService firebasePush;

  @Test
  void notifyChatMessageStoresInAppNotificationAndRequestsPush() {
    User recipient = UserFixture.activeUser();
    ReflectionTestUtils.setField(recipient, "id", 2L);
    when(notifications.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service().notifyChatMessage(recipient, "보낸이", "안녕하세요", 10L);

    assertThat(response.type()).isEqualTo(com.fanroute.sync.domain.notification.entity.NotificationType.CHAT_MESSAGE);
    assertThat(response.resourceId()).isEqualTo(10L);
    verify(firebasePush).sendChatMessage(2L, "보낸이님의 새 메시지", "안녕하세요", 10L);
  }

  private NotificationService service() {
    return new NotificationService(notifications, tokens, firebasePush);
  }
}
