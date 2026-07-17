package com.fanroute.sync.global.external;

import java.net.URI;
import java.util.Objects;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

/**
 * 공통 {@link RestClient.Builder} 설정을 기반으로 외부 API별 HTTP Interface Proxy를 생성하는 Factory
 * <p>
 * 각 클라이언트를 생성할 때 Builder를 복제하여 사용하므로, 서비스별 Base URL 설정이 다른 외부 API 클라이언트에 영향을 주지 않습니다.
 * </p>
 */
@RequiredArgsConstructor
public class ExternalApiClientFactory {

  @NotNull
  private final RestClient.Builder restClientBuilder;

  /**
   * 지정한 외부 API Base URL을 사용하는 HTTP Interface Proxy를 생성
   *
   * @param clientType {@code @HttpExchange} 기반 HTTP Interface 타입
   * @param baseUrl    외부 API의 기본 URL
   * @param <T>        생성할 HTTP Interface 타입
   * @return 생성된 HTTP Interface Proxy
   * @throws NullPointerException clientType 또는 baseUrl이 null인 경우
   */
  public <T> T createClient(@NotNull Class<T> clientType, @NotNull URI baseUrl) {
    Objects.requireNonNull(clientType, "clientType must not be null");

    RestClient restClient = restClientBuilder.clone()
        .baseUrl(baseUrl.toString())
        .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builderFor(adapter).build();
    return proxyFactory.createClient(clientType);
  }
}