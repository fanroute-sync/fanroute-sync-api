package com.fanroute.sync.domain.schedule.client;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fanroute.sync.domain.schedule.config.GeminiProperties;
import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class GeminiRestClient {

  private static final Map<String, Object> ITINERARY_SCHEMA = createItinerarySchema();

  private final RestClient restClient;
  private final GeminiProperties properties;
  private final ObjectMapper objectMapper;

  public GeminiRestClient(@Qualifier("geminiRestClient") RestClient restClient,
      GeminiProperties properties, ObjectMapper objectMapper) {
    this.restClient = restClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public GeminiDto.GeneratedItinerary generate(AiItineraryGenerationDto.GenerationInput input) {
    if (!StringUtils.hasText(properties.getApiKey())) {
      throw new IllegalStateException("GEMINI_API_KEY is not configured");
    }

    GeminiDto.GenerateContentResponse response = restClient.post()
        .uri("/v1beta/models/{model}:generateContent", properties.getModel())
        .body(new GeminiDto.GenerateContentRequest(
            List.of(new GeminiDto.Content(List.of(new GeminiDto.Part(createPrompt(input))))),
            new GeminiDto.GenerationConfig(new GeminiDto.ResponseFormat(
                new GeminiDto.StructuredText("APPLICATION_JSON", ITINERARY_SCHEMA)))))
        .retrieve()
        .body(GeminiDto.GenerateContentResponse.class);

    try {
      return objectMapper.readValue(extractText(response), GeminiDto.GeneratedItinerary.class);
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("Gemini response JSON is invalid", exception);
    }
  }

  private String extractText(GeminiDto.GenerateContentResponse response) {
    if (response == null || response.candidates() == null || response.candidates().isEmpty()
        || response.candidates().getFirst().content() == null
        || response.candidates().getFirst().content().parts() == null
        || response.candidates().getFirst().content().parts().isEmpty()) {
      throw new IllegalArgumentException("Gemini response does not contain generated content");
    }
    String text = response.candidates().getFirst().content().parts().getFirst().text();
    if (!StringUtils.hasText(text)) {
      throw new IllegalArgumentException("Gemini response content is empty");
    }
    return text;
  }

  private String createPrompt(AiItineraryGenerationDto.GenerationInput input) {
    String fixedItems = input.fixedItems().stream()
        .map(item -> "%s %s (%d분)".formatted(item.scheduledTime(), item.title(),
            item.durationMinutes() == null ? 0 : item.durationMinutes()))
        .toList().toString();
    String placeCandidates = input.placeCandidates().stream()
        .map(place -> "id=%d, name=%s, address=%s".formatted(place.id(), place.name(),
            place.address() == null ? "" : place.address()))
        .toList().toString();

    return """
        너는 부산 공연 여행의 하루 일정을 만드는 도우미다.
        대상 날짜: %s
        여행 기간: %s ~ %s
        여행 강도: %s
        동행: %s
        선호: %s
        고정 일정: %s
        장소 후보: %s

        고정 일정과 겹치지 않는 일반 일정만 생성해라. 고정 일정은 결과에 포함하지 마라.
        장소 후보 중 선택한 장소는 해당 placeId만 넣고, 후보 외 장소는 placeId를 null로 둬라.
        time은 HH:mm, durationMinutes는 1 이상의 정수로 반환해라.
        응답은 제공된 JSON 스키마만 만족해야 한다.
        """.formatted(input.date(), input.arrivalAt(), input.departureAt(),
        input.travelIntensity(), input.companions(), input.preferences(), fixedItems,
        placeCandidates);
  }

  private static Map<String, Object> createItinerarySchema() {
    Map<String, Object> itemSchema = Map.of(
        "type", "object",
        "properties", Map.of(
            "time", Map.of("type", "string"),
            "title", Map.of("type", "string"),
            "durationMinutes", Map.of("type", "integer"),
            "placeId", Map.of("type", List.of("integer", "null"))),
        "required", List.of("time", "title", "durationMinutes", "placeId"));
    return Map.of(
        "type", "object",
        "properties", Map.of("items", Map.of("type", "array", "items", itemSchema)),
        "required", List.of("items"));
  }
}
