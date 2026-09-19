package com.fanroute.sync.domain.schedule.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.local")
public record KakaoLocalProperties(String restApiKey, URI baseUrl) {

  public boolean enabled() {
    return restApiKey != null && !restApiKey.isBlank();
  }
}
