package com.fanroute.sync.domain.schedule.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.client.RestClient;

import com.fanroute.sync.domain.schedule.client.GeminiRestClient;
import com.fanroute.sync.global.external.ExternalApiLoggingInterceptor;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class AiItineraryGenerationConfigTest {

  @Test
  @DisplayName("Gemini HTTP 클라이언트와 API 클라이언트를 서로 다른 이름으로 등록한다")
  void registersGeminiHttpClientWithoutNameConflict() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(AiItineraryGenerationConfig.class, GeminiRestClient.class);
      context.registerBean(ExternalApiLoggingInterceptor.class);
      context.registerBean(ObjectMapper.class, () -> JsonMapper.builder().build());
      context.refresh();

      assertThat(context.containsBean("geminiHttpClient")).isTrue();
      assertThat(context.containsBean("geminiRestClient")).isTrue();
      assertThat(context.getBean("geminiHttpClient")).isInstanceOf(RestClient.class);
      assertThat(context.getBean(GeminiRestClient.class)).isNotNull();
    }
  }
}
