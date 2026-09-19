package com.fanroute.sync.domain.schedule.client;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fanroute.sync.domain.schedule.config.KakaoLocalProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KakaoLocalClient {

  private final RestClient restClient;
  private final KakaoLocalProperties properties;

  public KakaoLocalClient(@Qualifier("kakaoLocalHttpClient") RestClient restClient,
      KakaoLocalProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  public Optional<Coordinates> findCoordinates(String query) {
    if (!properties.enabled() || query == null || query.isBlank()) {
      return Optional.empty();
    }
    return request("/v2/local/search/address.json", query)
        .or(() -> request("/v2/local/search/keyword.json", query));
  }

  private Optional<Coordinates> request(String path, String query) {
    try {
      KakaoLocalResponse response = restClient.get().uri(uriBuilder -> uriBuilder.path(path)
          .queryParam("query", query).build()).retrieve().body(KakaoLocalResponse.class);
      if (response == null || response.documents() == null || response.documents().isEmpty()) {
        return Optional.empty();
      }
      KakaoLocalDocument document = response.documents().getFirst();
      return Optional.of(new Coordinates(Double.parseDouble(document.latitude()),
          Double.parseDouble(document.longitude())));
    } catch (RuntimeException exception) {
      log.warn("Kakao Local coordinate lookup failed: path={}, exception={}", path,
          exception.getClass().getSimpleName());
      return Optional.empty();
    }
  }

  public record Coordinates(Double latitude, Double longitude) {
  }

  private record KakaoLocalResponse(List<KakaoLocalDocument> documents) {
  }

  private record KakaoLocalDocument(String x, String y) {
    private String longitude() {
      return x;
    }

    private String latitude() {
      return y;
    }
  }
}
