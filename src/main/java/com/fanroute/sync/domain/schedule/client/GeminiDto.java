package com.fanroute.sync.domain.schedule.client;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeminiDto {

  public record GenerateContentRequest(
      List<Content> contents,
      GenerationConfig generationConfig) {
  }

  public record Content(List<Part> parts) {
  }

  public record Part(String text) {
  }

  public record GenerationConfig(ResponseFormat responseFormat) {
  }

  public record ResponseFormat(StructuredText text) {
  }

  public record StructuredText(String mimeType, Map<String, Object> schema) {
  }

  public record GenerateContentResponse(List<Candidate> candidates) {
  }

  public record Candidate(Content content) {
  }

  public record GeneratedItinerary(List<GeneratedItem> items) {
  }

  public record GeneratedItem(
      @JsonProperty("time") String scheduledTime,
      String title,
      Integer durationMinutes,
      Long placeId) {
  }
}
