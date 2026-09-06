package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import com.fanroute.sync.domain.schedule.config.AiGenerationRetryProperties;
import com.fanroute.sync.global.external.ExternalApiErrorType;
import com.fanroute.sync.global.external.ExternalApiException;

class AiGenerationRetryPolicyTest {

  @Test
  @DisplayName("429, 5xx와 network 오류만 재시도한다")
  void classifiesRetryableFailures() {
    AiGenerationRetryPolicy policy = policy();

    assertThat(policy.isRetryable(response(HttpStatus.TOO_MANY_REQUESTS))).isTrue();
    assertThat(policy.isRetryable(response(HttpStatus.SERVICE_UNAVAILABLE))).isTrue();
    assertThat(policy.isRetryable(new ResourceAccessException("timeout",
        new SocketTimeoutException()))).isTrue();
    assertThat(policy.isRetryable(response(HttpStatus.BAD_REQUEST))).isFalse();
    assertThat(policy.isRetryable(new IllegalArgumentException("invalid json"))).isFalse();
  }

  @Test
  @DisplayName("backoff는 지수 증가하고 jitter와 최대 지연을 적용한다")
  void calculatesExponentialBackoffWithJitterAndCap() {
    AiGenerationRetryPolicy policy = policy();

    assertThat(policy.nextDelay(1, -1)).isEqualTo(Duration.ofMillis(800));
    assertThat(policy.nextDelay(2, 0)).isEqualTo(Duration.ofSeconds(2));
    assertThat(policy.nextDelay(10, 1)).isEqualTo(Duration.ofSeconds(10));
  }

  private AiGenerationRetryPolicy policy() {
    AiGenerationRetryProperties properties = new AiGenerationRetryProperties();
    properties.setInitialDelay(Duration.ofSeconds(1));
    properties.setMaxDelay(Duration.ofSeconds(10));
    properties.setJitterRatio(0.2);
    return new AiGenerationRetryPolicy(properties);
  }

  private ExternalApiException response(HttpStatus status) {
    ExternalApiErrorType errorType = status.is5xxServerError()
        ? ExternalApiErrorType.SERVER_ERROR : ExternalApiErrorType.CLIENT_ERROR;
    return ExternalApiException.responseError(errorType, status, "Gemini error");
  }
}
