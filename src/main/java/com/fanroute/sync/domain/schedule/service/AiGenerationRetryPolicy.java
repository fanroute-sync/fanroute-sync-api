package com.fanroute.sync.domain.schedule.service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import com.fanroute.sync.domain.schedule.config.AiGenerationRetryProperties;
import com.fanroute.sync.global.external.ExternalApiErrorType;
import com.fanroute.sync.global.external.ExternalApiException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiGenerationRetryPolicy {

  private final AiGenerationRetryProperties properties;

  public boolean isRetryable(Throwable throwable) {
    if (throwable instanceof ExternalApiException externalApiException) {
      return externalApiException.getErrorType() == ExternalApiErrorType.CONNECT_TIMEOUT
          || externalApiException.getErrorType() == ExternalApiErrorType.READ_TIMEOUT
          || externalApiException.getErrorType() == ExternalApiErrorType.CONNECTION_ERROR
          || externalApiException.getStatusCode() != null
              && (externalApiException.getStatusCode().value() == 429
                  || externalApiException.getStatusCode().is5xxServerError());
    }
    return throwable instanceof ResourceAccessException;
  }

  public boolean canRetry(int attemptCount) {
    return attemptCount < properties.getMaxAttempts();
  }

  public Duration nextDelay(int attemptCount) {
    double jitter = ThreadLocalRandom.current().nextDouble(-1, 1);
    return nextDelay(attemptCount, jitter);
  }

  Duration nextDelay(int attemptCount, double jitterSample) {
    long initialMillis = properties.getInitialDelay().toMillis();
    int exponent = Math.max(0, Math.min(attemptCount - 1, 62));
    long exponentialMillis;
    try {
      exponentialMillis = Math.multiplyExact(initialMillis, 1L << exponent);
    } catch (ArithmeticException exception) {
      exponentialMillis = Long.MAX_VALUE;
    }
    double multiplier = 1 + properties.getJitterRatio() * jitterSample;
    long jitteredMillis = Math.max(1, Math.round(exponentialMillis * multiplier));
    return Duration.ofMillis(Math.min(jitteredMillis, properties.getMaxDelay().toMillis()));
  }
}
