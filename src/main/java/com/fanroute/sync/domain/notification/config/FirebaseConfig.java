package com.fanroute.sync.domain.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class FirebaseConfig {
  @Bean
  FirebaseApp firebaseApp() throws java.io.IOException {
    if (!FirebaseApp.getApps().isEmpty()) return FirebaseApp.getInstance();
    return FirebaseApp.initializeApp(FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.getApplicationDefault()).build());
  }
}
