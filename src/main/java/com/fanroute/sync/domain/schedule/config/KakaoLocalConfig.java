package com.fanroute.sync.domain.schedule.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.fanroute.sync.global.external.ExternalApiLoggingInterceptor;

@Configuration
@EnableConfigurationProperties(KakaoLocalProperties.class)
public class KakaoLocalConfig {

  @Bean("kakaoLocalHttpClient")
  public RestClient kakaoLocalHttpClient(KakaoLocalProperties properties,
      ExternalApiLoggingInterceptor loggingInterceptor) {
    RestClient.Builder builder = RestClient.builder()
        .baseUrl(properties.baseUrl().toString())
        .requestInterceptor(loggingInterceptor);
    if (properties.enabled()) {
      builder.defaultHeader("Authorization", "KakaoAK " + properties.restApiKey());
    }
    return builder.build();
  }
}
