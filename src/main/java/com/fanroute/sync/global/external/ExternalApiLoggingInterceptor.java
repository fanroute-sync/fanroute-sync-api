package com.fanroute.sync.global.external;

import java.io.IOException;
import java.net.URI;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 외부 API 요청과 응답의 기본 정보를 기록하고, 네트워크 전송 오류를 {@link ExternalApiException}으로 변환합니다.
 * <p>
 * API Key, OAuth 인가 코드, Access Token 등의 노출을 방지하기 위해 Query String, Header 및 요청·응답 Body는 로그에 기록하지
 * 않습니다.
 * </p>
 */
@Slf4j
public class ExternalApiLoggingInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    String target = sanitizedTarget(request.getURI());
    long startTime = System.nanoTime();
    log.debug("External API request: method={}, target={}", request.getMethod(), target);

    try {
      ClientHttpResponse response = execution.execute(request, body);
      log.debug("External API response: method={}, target={}, status={}, elapsedMs={}",
          request.getMethod(), target, response.getStatusCode().value(), elapsedMillis(startTime));
      return response;
    } catch (IOException exception) {
      log.warn("External API transport failure: method={}, target={}, elapsedMs={}, exception={}",
          request.getMethod(), target, elapsedMillis(startTime),
          exception.getClass().getSimpleName());
      throw ExternalApiTransportExceptionMapper.convert(exception);
    }
  }

  /**
   * Query String과 사용자 정보를 제외한 안전한 요청 대상만 반환합니다.
   */
  private static String sanitizedTarget(URI uri) {
    if (uri == null) {
      return "unknown";
    }

    String scheme = uri.getScheme();
    String host = uri.getHost();

    if (scheme == null || host == null) {
      String path = uri.getPath();
      return path == null || path.isBlank() ? "unknown" : path;
    }

    String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
    String path = uri.getPath() == null ? "" : uri.getPath();
    return scheme + "://" + host + port + path;
  }

  private static long elapsedMillis(long startTime) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
  }
}