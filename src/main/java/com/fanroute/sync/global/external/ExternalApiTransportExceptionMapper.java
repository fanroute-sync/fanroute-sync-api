package com.fanroute.sync.global.external;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * HTTP Client에서 발생한 저수준 I/O 예외를 외부 API 공통 예외로 변환합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExternalApiTransportExceptionMapper {

  /**
   * 예외 자신과 cause chain을 확인하여 전송 실패 원인을 분류합니다.
   *
   * @param exception HTTP Client에서 발생한 I/O 예외
   * @return 분류된 외부 API 예외
   */
  public static ExternalApiException convert(IOException exception) {
    Throwable cause = exception;
    while (cause != null) {
      // HttpConnectTimeoutException은 HttpTimeoutException의 하위 타입이므로 먼저 검사
      if (cause instanceof HttpConnectTimeoutException) {
        return ExternalApiException.transportError(ExternalApiErrorType.CONNECT_TIMEOUT,
            "External API connection timed out", exception);
      }
      if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
        return ExternalApiException.transportError(ExternalApiErrorType.READ_TIMEOUT,
            "External API response timed out", exception);
      }
      if (cause instanceof ConnectException) {
        return ExternalApiException.transportError(ExternalApiErrorType.CONNECTION_ERROR,
            "External API connection failed", exception);
      }
      cause = cause.getCause();
    }
    return ExternalApiException.transportError(ExternalApiErrorType.CONNECTION_ERROR,
        "External API transport failed", exception);
  }
}