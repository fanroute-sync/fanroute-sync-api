package com.fanroute.sync.domain.schedule.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;

class GeminiPropertiesTest {

  @Test
  @DisplayName("Gemini 추론 수준 기본값은 MINIMAL이고 LOW로 변경할 수 있다")
  void defaultsToMinimalAndSupportsLow() {
    GeminiProperties properties = new GeminiProperties();

    assertThat(properties.getThinkingLevel())
        .isEqualTo(GeminiProperties.ThinkingLevel.MINIMAL);

    properties.setThinkingLevel(GeminiProperties.ThinkingLevel.LOW);

    assertThat(properties.getThinkingLevel())
        .isEqualTo(GeminiProperties.ThinkingLevel.LOW);
  }

  @Test
  @DisplayName("Gemini 추론 수준은 비어 있을 수 없다")
  void rejectsMissingThinkingLevel() {
    GeminiProperties properties = new GeminiProperties();
    properties.setThinkingLevel(null);

    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validateProperty(properties, "thinkingLevel"))
          .isNotEmpty();
    }
  }
}
