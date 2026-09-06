package com.fanroute.sync.domain.schedule.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiGenerationStreamPropertiesTest {

  @Test
  @DisplayName("pending claim idle은 processing lease보다 짧을 수 없다")
  void rejectsClaimIdleShorterThanProcessingLease() {
    AiGenerationStreamProperties properties = new AiGenerationStreamProperties();
    properties.setProcessingLease(Duration.ofSeconds(60));
    properties.setClaimMinIdle(Duration.ofSeconds(30));

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("claim min idle");
  }

  @Test
  @DisplayName("worker concurrency는 1 이상이어야 한다")
  void rejectsNonPositiveWorkerConcurrency() {
    AiGenerationStreamProperties properties = new AiGenerationStreamProperties();
    properties.setWorkerConcurrency(0);

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("worker concurrency");
  }
}
