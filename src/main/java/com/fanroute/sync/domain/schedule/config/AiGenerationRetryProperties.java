package com.fanroute.sync.domain.schedule.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai-generation.retry")
public class AiGenerationRetryProperties {

  private int maxAttempts = 3;
  private Duration initialDelay = Duration.ofSeconds(2);
  private Duration maxDelay = Duration.ofMinutes(1);
  private double jitterRatio = 0.2;

  @PostConstruct
  void validate() {
    if (maxAttempts < 1) {
      throw new IllegalStateException("AI generation max attempts must be at least 1");
    }
    if (initialDelay.isZero() || initialDelay.isNegative()) {
      throw new IllegalStateException("AI generation initial retry delay must be positive");
    }
    if (maxDelay.compareTo(initialDelay) < 0) {
      throw new IllegalStateException(
          "AI generation max retry delay must be greater than or equal to initial delay");
    }
    if (jitterRatio < 0 || jitterRatio >= 1) {
      throw new IllegalStateException("AI generation retry jitter ratio must be in [0, 1)");
    }
  }
}
