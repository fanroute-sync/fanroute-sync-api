package com.fanroute.sync.domain.schedule.client;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fanroute.sync.domain.schedule.config.GeminiProperties;

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

  public record GenerationConfig(ResponseFormat responseFormat, ThinkingConfig thinkingConfig) {
  }

  public record ThinkingConfig(GeminiProperties.ThinkingLevel thinkingLevel) {
  }

  public record ResponseFormat(StructuredText text) {
  }

  public record StructuredText(String mimeType, Map<String, Object> schema) {
  }

  public record GenerateContentResponse(List<Candidate> candidates, UsageMetadata usageMetadata) {
  }

  public record UsageMetadata(
      Integer promptTokenCount,
      Integer cachedContentTokenCount,
      Integer candidatesTokenCount,
      Integer thoughtsTokenCount,
      Integer totalTokenCount) {
  }

  public record Candidate(Content content, String finishReason) {
  }

  public record GeneratedItinerary(List<GeneratedItem> items) {
  }

  public record SelectedPlaceIds(List<Long> placeIds) {
  }

  public record StageUsage(String stage, UsageMetadata usageMetadata) {
  }

  public record GenerationResult(
      GeneratedItinerary itinerary,
      UsageMetadata usageMetadata,
      List<StageUsage> stageUsages) {

    public GenerationResult(GeneratedItinerary itinerary, UsageMetadata usageMetadata) {
      this(itinerary, usageMetadata, List.of(new StageUsage("itinerary", usageMetadata)));
    }
  }

  public record GeneratedItem(
      @JsonProperty("time") String scheduledTime,
      String title,
      Integer durationMinutes,
      Long placeId) {
  }
}
