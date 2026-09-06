package com.fanroute.sync.domain.schedule.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai-generation.notification")
public class AiGenerationNotificationProperties {

  private boolean enabled;
  private int batchSize = 20;
  private Duration pollDelay = Duration.ofSeconds(1);
  private Duration processingLease = Duration.ofSeconds(30);
  private int maxAttempts = 3;
  private Duration initialDelay = Duration.ofSeconds(2);
  private Duration maxDelay = Duration.ofMinutes(1);
  private double jitterRatio = 0.2;

  @PostConstruct
  void validate() {
    if (batchSize < 1 || maxAttempts < 1) {
      throw new IllegalStateException("AI notification batch size and max attempts must be positive");
    }
    if (processingLease.isZero() || processingLease.isNegative()
        || initialDelay.isZero() || initialDelay.isNegative()) {
      throw new IllegalStateException("AI notification lease and retry delay must be positive");
    }
    if (maxDelay.compareTo(initialDelay) < 0) {
      throw new IllegalStateException(
          "AI notification max delay must be greater than or equal to initial delay");
    }
    if (jitterRatio < 0 || jitterRatio >= 1) {
      throw new IllegalStateException("AI notification retry jitter ratio must be in [0, 1)");
    }
  }
}
