package com.fanroute.sync.domain.place.config;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fanroute.sync.domain.place.client.TourApiClient;
import com.fanroute.sync.global.external.ExternalApiClientFactory;
import com.fanroute.sync.global.external.ExternalApiProperties;

@Configuration
@EnableConfigurationProperties(TourApiProperties.class)
public class TourApiConfig {

  @Bean
  public TourApiClient tourApiClient(
      ExternalApiClientFactory factory, ExternalApiProperties externalApiProperties) {
    URI baseUrl = externalApiProperties.getRequiredService("tour").getBaseUrl();
    return factory.createClient(TourApiClient.class, baseUrl);
  }
}
