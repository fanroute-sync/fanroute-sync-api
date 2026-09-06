package com.fanroute.sync.domain.schedule.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiGenerationRetryPropertiesTest {

  @Test
  @DisplayName("최대 backoff가 최초 backoff보다 짧으면 시작을 거부한다")
  void rejectsMaxDelayShorterThanInitialDelay() {
    AiGenerationRetryProperties properties = new AiGenerationRetryProperties();
    properties.setInitialDelay(Duration.ofSeconds(10));
    properties.setMaxDelay(Duration.ofSeconds(5));

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("jitter 비율은 0 이상 1 미만이어야 한다")
  void rejectsInvalidJitterRatio() {
    AiGenerationRetryProperties properties = new AiGenerationRetryProperties();
    properties.setJitterRatio(1);

    assertThatThrownBy(properties::validate)
        .isInstanceOf(IllegalStateException.class);
  }
}
