package com.fanroute.sync.domain.schedule.client;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
  private static final Map<String, Object> PLACE_SELECTION_SCHEMA = createPlaceSelectionSchema();
  private static final String COMMON_INSTRUCTION = """
      너는 부산 공연 여행 일정을 돕는 전문가다.
      제공된 후보와 고정 일정만 근거로 판단하고, 사실을 추측하지 마라.
      응답은 제공된 JSON 스키마만 만족해야 한다.
      """;

  private final RestClient restClient;
  private final GeminiProperties properties;
  private final ObjectMapper objectMapper;

  public GeminiRestClient(@Qualifier("geminiHttpClient") RestClient restClient,
      GeminiProperties properties, ObjectMapper objectMapper) {
    this.restClient = restClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public GeminiDto.GenerationResult generate(AiItineraryGenerationDto.GenerationInput input) {
    if (!StringUtils.hasText(properties.getApiKey())) {
      throw new IllegalStateException("GEMINI_API_KEY is not configured");
    }

    if (input.placeCandidates().size() < properties.getPromptChainCandidateThreshold()) {
      return generateItinerary(input, "itinerary");
    }

    GeminiDto.GenerateContentResponse selectionResponse;
    GeminiDto.SelectedPlaceIds selection;
    try {
      selectionResponse = request(createSelectionPrompt(input), PLACE_SELECTION_SCHEMA);
      selection = read(selectionResponse, GeminiDto.SelectedPlaceIds.class);
    } catch (IllegalArgumentException exception) {
      return generateItinerary(input, "itinerary-fallback");
    }
    List<AiItineraryGenerationDto.PlaceCandidate> selectedCandidates = selectedCandidates(input,
        selection == null ? List.of() : selection.placeIds());
    if (selectedCandidates.isEmpty()) {
      return generateItinerary(input, "itinerary-fallback");
    }

    GeminiDto.GenerationResult itineraryResult = generateItinerary(withCandidates(input,
        selectedCandidates), "itinerary");
    GeminiDto.UsageMetadata totalUsage = add(selectionResponse.usageMetadata(),
        itineraryResult.usageMetadata());
    return new GeminiDto.GenerationResult(itineraryResult.itinerary(), totalUsage,
        List.of(new GeminiDto.StageUsage("candidate-selection", selectionResponse.usageMetadata()),
            itineraryResult.stageUsages().getFirst()));
  }

  private GeminiDto.GenerationResult generateItinerary(AiItineraryGenerationDto.GenerationInput input,
      String stage) {
    GeminiDto.GenerateContentResponse response = request(createItineraryPrompt(input), ITINERARY_SCHEMA);
    return new GeminiDto.GenerationResult(read(response, GeminiDto.GeneratedItinerary.class),
        response.usageMetadata(), List.of(new GeminiDto.StageUsage(stage, response.usageMetadata())));
  }

  private GeminiDto.GenerateContentResponse request(String prompt, Map<String, Object> schema) {
    return restClient.post()
        .uri("/v1beta/models/{model}:generateContent", properties.getModel())
        .body(new GeminiDto.GenerateContentRequest(
            List.of(new GeminiDto.Content(List.of(new GeminiDto.Part(prompt)))),
            new GeminiDto.GenerationConfig(new GeminiDto.ResponseFormat(
                new GeminiDto.StructuredText("APPLICATION_JSON", schema)))))
        .retrieve()
        .body(GeminiDto.GenerateContentResponse.class);
  }

  private <T> T read(GeminiDto.GenerateContentResponse response, Class<T> type) {
    try {
      return objectMapper.readValue(extractText(response), type);
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

  private String createSelectionPrompt(AiItineraryGenerationDto.GenerationInput input) {
    return "%s\n%s\n후보 중 하루 일정에 적합한 장소 ID를 최대 8개 선택해라. 후보 외 ID는 반환하지 마라."
        .formatted(COMMON_INSTRUCTION, dynamicContext(input, input.placeCandidates()));
  }

  private String createItineraryPrompt(AiItineraryGenerationDto.GenerationInput input) {
    return "%s\n%s\n고정 일정과 겹치지 않는 일반 일정만 생성해라. 고정 일정은 결과에 포함하지 마라.\n"
        .formatted(COMMON_INSTRUCTION, dynamicContext(input, input.placeCandidates()))
        + "이미 등록된 장소는 후보에서 제외되어 있으므로, 장소 후보를 중복해서 선택하지 마라.\n"
        + "장소 후보 중 선택한 장소는 해당 placeId만 넣고, 후보 외 장소는 placeId를 null로 둬라.\n"
        + "time은 HH:mm, durationMinutes는 1 이상의 정수로 반환해라.";
  }

  private String dynamicContext(AiItineraryGenerationDto.GenerationInput input,
      List<AiItineraryGenerationDto.PlaceCandidate> candidates) {
    String fixedItems = input.fixedItems().stream()
        .map(item -> "%s %s (%d분)".formatted(item.scheduledTime(), item.title(),
            item.durationMinutes() == null ? 0 : item.durationMinutes()))
        .toList().toString();
    String placeCandidates = candidates.stream()
        .map(place -> "id=%d, name=%s, address=%s".formatted(place.id(), place.name(),
            place.address() == null ? "" : place.address()))
        .toList().toString();

    return """
        대상 날짜: %s
        여행 기간: %s ~ %s
        여행 강도: %s
        동행: %s
        선호: %s
        고정 일정: %s
        장소 후보: %s
        """.formatted(input.date(), input.arrivalAt(), input.departureAt(),
        input.travelIntensity(), input.companions(), input.preferences(), fixedItems,
        placeCandidates);
  }

  private List<AiItineraryGenerationDto.PlaceCandidate> selectedCandidates(
      AiItineraryGenerationDto.GenerationInput input, List<Long> selectedIds) {
    if (selectedIds == null || selectedIds.isEmpty()) {
      return List.of();
    }
    Set<Long> validIds = input.placeCandidates().stream()
        .map(AiItineraryGenerationDto.PlaceCandidate::id)
        .collect(Collectors.toSet());
    Set<Long> uniqueIds = selectedIds.stream()
        .filter(validIds::contains)
        .limit(8)
        .collect(Collectors.toSet());
    return input.placeCandidates().stream().filter(candidate -> uniqueIds.contains(candidate.id()))
        .toList();
  }

  private AiItineraryGenerationDto.GenerationInput withCandidates(
      AiItineraryGenerationDto.GenerationInput input,
      List<AiItineraryGenerationDto.PlaceCandidate> candidates) {
    return new AiItineraryGenerationDto.GenerationInput(input.date(), input.arrivalAt(),
        input.departureAt(), input.travelIntensity(), input.companions(), input.preferences(),
        input.fixedItems(), candidates);
  }

  private GeminiDto.UsageMetadata add(GeminiDto.UsageMetadata left,
      GeminiDto.UsageMetadata right) {
    if (left == null) {
      return right;
    }
    if (right == null) {
      return left;
    }
    return new GeminiDto.UsageMetadata(add(left.promptTokenCount(), right.promptTokenCount()),
        add(left.cachedContentTokenCount(), right.cachedContentTokenCount()),
        add(left.candidatesTokenCount(), right.candidatesTokenCount()),
        add(left.thoughtsTokenCount(), right.thoughtsTokenCount()),
        add(left.totalTokenCount(), right.totalTokenCount()));
  }

  private Integer add(Integer left, Integer right) {
    return (left == null ? 0 : left) + (right == null ? 0 : right);
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

  private static Map<String, Object> createPlaceSelectionSchema() {
    return Map.of(
        "type", "object",
        "properties", Map.of("placeIds", Map.of("type", "array", "items", Map.of(
            "type", "integer"))),
        "required", List.of("placeIds"));
  }
}
