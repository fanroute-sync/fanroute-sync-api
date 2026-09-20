package com.fanroute.sync.domain.schedule.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

  private String apiKey = "";
  private String model = "gemini-3.5-flash-lite";
  @NotNull
  private ThinkingLevel thinkingLevel = ThinkingLevel.MINIMAL;
  private URI baseUrl = URI.create("https://generativelanguage.googleapis.com");
  private Duration connectTimeout = Duration.ofSeconds(3);
  private Duration readTimeout = Duration.ofSeconds(30);
  private int corePoolSize = 2;
  private int maxPoolSize = 4;
  private int queueCapacity = 20;

  public enum ThinkingLevel {
    MINIMAL, LOW, MEDIUM, HIGH
  }
}
