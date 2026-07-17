package com.fanroute.sync.global.external;

import java.util.Objects;

import org.springframework.http.HttpStatusCode;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 외부 API에서 발생한 공통 예외
 */
@Getter
public class ExternalApiException extends RuntimeException {

  private final ExternalApiErrorType errorType;
  private final HttpStatusCode statusCode; // timeout의 경우 null

  public ExternalApiException(
      @NotNull ExternalApiErrorType errorType, HttpStatusCode statusCode, String message,
      Throwable cause) {
    super(message, cause);
    this.errorType = errorType;
    this.statusCode = statusCode;
  }

  /**
   * 외부 API가 4xx 또는 5xx 응답을 반환한 경우의 예외를 생성합니다.
   */
  public static ExternalApiException responseError(ExternalApiErrorType errorType,
      HttpStatusCode statusCode, String message) {
    return new ExternalApiException(errorType,
        Objects.requireNonNull(statusCode, "statusCode must not be null"), message, null);
  }

  /**
   * timeout 또는 연결 실패처럼 HTTP 응답을 받지 못한 경우의 * 전송 예외를 생성합니다.
   */
  public static ExternalApiException transportError(ExternalApiErrorType errorType, String message,
      Throwable cause) {
    return new ExternalApiException(errorType, null, message,
        Objects.requireNonNull(cause, "cause must not be null"));
  }
}