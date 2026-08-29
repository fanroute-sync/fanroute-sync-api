package com.fanroute.sync.domain.schedule.config;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import com.fanroute.sync.global.external.ExternalApiErrorType;
import com.fanroute.sync.global.external.ExternalApiException;
import com.fanroute.sync.global.external.ExternalApiLoggingInterceptor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(GeminiProperties.class)
public class AiItineraryGenerationConfig {

  @Bean("geminiRestClient")
  public RestClient geminiRestClient(GeminiProperties properties,
      ExternalApiLoggingInterceptor loggingInterceptor) {
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(properties.getConnectTimeout())
        .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.getReadTimeout());

    RestClient.Builder builder = RestClient.builder()
        .baseUrl(properties.getBaseUrl().toString())
        .requestFactory(requestFactory)
        .requestInterceptor(loggingInterceptor)
        .defaultStatusHandler(HttpStatusCode::is4xxClientError,
            (request, response) -> {
              throw ExternalApiException.responseError(ExternalApiErrorType.CLIENT_ERROR,
                  response.getStatusCode(), "Gemini API returned a client error");
            })
        .defaultStatusHandler(HttpStatusCode::is5xxServerError,
            (request, response) -> {
              throw ExternalApiException.responseError(ExternalApiErrorType.SERVER_ERROR,
                  response.getStatusCode(), "Gemini API returned a server error");
            });
    if (!properties.getApiKey().isBlank()) {
      builder.defaultHeader("x-goog-api-key", properties.getApiKey());
    }
    return builder.build();
  }

  @Bean("aiItineraryGenerationExecutor")
  public ThreadPoolTaskExecutor aiItineraryGenerationExecutor(GeminiProperties properties) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(properties.getCorePoolSize());
    executor.setMaxPoolSize(properties.getMaxPoolSize());
    executor.setQueueCapacity(properties.getQueueCapacity());
    executor.setThreadNamePrefix("ai-itinerary-");
    executor.initialize();
    return executor;
  }
}
