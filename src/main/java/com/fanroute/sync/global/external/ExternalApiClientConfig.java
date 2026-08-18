package com.fanroute.sync.global.external;

import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 외부 API 호출에 사용할 공통 HTTP 클라이언트 구성
 * <p>
 * 외부 서비스별 Base URL과 API 인터페이스는 {@link ExternalApiClientFactory}에서 설정.
 * </p>
 * 구성 내용:
 * <ul>
 * <li>연결 및 응답 timeout</li>
 * <li>요청·응답 로깅 인터셉터</li>
 * <li>4xx·5xx 응답의 공통 예외 변환</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(ExternalApiProperties.class)
public class ExternalApiClientConfig {

  /**
   * 외부 API용 인터셉터를 Bean으로 등록
   */
  @Bean
  public ExternalApiLoggingInterceptor externalApiLoggingInterceptor() {
    return new ExternalApiLoggingInterceptor();
  }

  /**
   * 외부 API 클라이언트 생성 시 사용할 공통 빌더
   *
   * @param properties         외부 API timeout 및 서비스 설정
   * @param loggingInterceptor 요청·응답 로깅 인터셉터
   * @return 외부 API 호출용 공통 RestClient Builder
   */
  @Bean("externalApiRestClientBuilder")
  public RestClient.Builder externalApiRestClientBuilder(ExternalApiProperties properties,
      ExternalApiLoggingInterceptor loggingInterceptor) {
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(properties.getHttp().getConnectTimeout())
        .build();

    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.getHttp().getReadTimeout());

    return RestClient.builder()
        .requestFactory(requestFactory)
        .requestInterceptor(loggingInterceptor)
        /* 외부 API의 4xx 응답을 서비스 공통 예외로 변환 */
        .defaultStatusHandler(
            HttpStatusCode::is4xxClientError,
            (request, response) -> {
              throw ExternalApiException.responseError(
                  ExternalApiErrorType.CLIENT_ERROR,
                  response.getStatusCode(),
                  "External API returned a client error");
            })
        /* 외부 API의 5xx 응답을 서비스 공통 예외로 변환 */
        .defaultStatusHandler(
            HttpStatusCode::is5xxServerError,
            (request, response) -> {
              throw ExternalApiException.responseError(
                  ExternalApiErrorType.SERVER_ERROR,
                  response.getStatusCode(),
                  "External API returned a server error");
            });
  }

  /**
   * 공통 RestClient 설정을 기반으로 서비스별 HTTP Interface Proxy 생성 Factory 등록
   *
   * @param builder 외부 API 전용 공통 RestClient Builder
   * @return 외부 API 클라이언트 Factory
   */
  @Bean
  public ExternalApiClientFactory externalApiClientFactory(
      @Qualifier("externalApiRestClientBuilder") RestClient.Builder builder) {
    return new ExternalApiClientFactory(builder);
  }
}
