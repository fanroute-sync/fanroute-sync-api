package com.fanroute.sync.domain.concert.config;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fanroute.sync.domain.concert.client.KopisClient;
import com.fanroute.sync.global.external.ExternalApiClientFactory;
import com.fanroute.sync.global.external.ExternalApiProperties;

@Configuration
@EnableConfigurationProperties(KopisProperties.class)
public class KopisConfig {

  @Bean
  public KopisClient kopisClient(
      ExternalApiClientFactory factory, ExternalApiProperties externalApiProperties) {
    URI baseUrl = externalApiProperties.getRequiredService("kopis").getBaseUrl();
    return factory.createClient(KopisClient.class, baseUrl);
  }
}
