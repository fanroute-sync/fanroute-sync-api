package com.fanroute.sync.domain.schedule.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai-generation.stream")
public class AiGenerationStreamProperties {

  private String key = "fanroute:ai-generation:v1";
  private String group = "ai-generation-workers";
  private String consumer = "${HOSTNAME:local}";
  private Duration outboxPollDelay = Duration.ofSeconds(1);
  private Duration publishLease = Duration.ofSeconds(30);
  private Duration processingLease = Duration.ofSeconds(45);
  private Duration claimMinIdle = Duration.ofSeconds(60);
  private Duration claimPollDelay = Duration.ofSeconds(1);
  private Duration readBlock = Duration.ofSeconds(1);
  private Duration consumerPollDelay = Duration.ofMillis(100);
  private int workerConcurrency = 2;

  @PostConstruct
  void validate() {
    if (processingLease.isZero() || processingLease.isNegative()) {
      throw new IllegalStateException("AI generation processing lease must be positive");
    }
    if (claimMinIdle.compareTo(processingLease) < 0) {
      throw new IllegalStateException(
          "AI generation claim min idle must be greater than or equal to processing lease");
    }
    if (readBlock.isZero() || readBlock.isNegative()) {
      throw new IllegalStateException("AI generation stream read block must be positive");
    }
    if (consumerPollDelay.isNegative()) {
      throw new IllegalStateException("AI generation consumer poll delay must not be negative");
    }
    if (workerConcurrency < 1) {
      throw new IllegalStateException("AI generation worker concurrency must be at least 1");
    }
  }
}
