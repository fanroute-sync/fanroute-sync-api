package com.fanroute.sync.domain.notification.service;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.fanroute.sync.domain.notification.repository.PushTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebasePushService {
  private final ObjectProvider<FirebaseApp> firebaseApp;
  private final PushTokenRepository tokens;

  @Async
  public void sendChatMessage(Long userId, String title, String content, Long roomId) {
    FirebaseApp app = firebaseApp.getIfAvailable();
    if (app == null) return;
    List<String> destinations = tokens.findByUserId(userId).stream().map(token -> token.getToken()).toList();
    for (String token : destinations) {
      try {
        Message message = Message.builder().setToken(token)
            .setNotification(Notification.builder().setTitle(title).setBody(content).build())
            .putData("type", "CHAT_MESSAGE").putData("roomId", roomId.toString()).build();
        FirebaseMessaging.getInstance(app).send(message);
      } catch (Exception exception) {
        log.warn("FCM push failed: userId={}", userId, exception);
      }
    }
  }
}
