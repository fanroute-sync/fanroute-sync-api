package com.fanroute.sync.domain.schedule.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fanroute.sync.domain.schedule.config.GeminiProperties;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.entity.PlaceTag;
import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.entity.TravelIntensityType;
import com.fanroute.sync.domain.schedule.entity.TravelMbtiType;

import tools.jackson.databind.json.JsonMapper;

class GeminiRestClientTest {

  @Test
  @DisplayName("Gemini REST API에 단일 구조화 출력 요청을 보낸다")
  void generatesStructuredItinerary() {
    RestClient.Builder builder = RestClient.builder()
        .baseUrl("https://gemini.example.com")
        .defaultHeader("x-goog-api-key", "test-key");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(
            "https://gemini.example.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("x-goog-api-key", "test-key"))
        .andExpect(content().string(containsString("\"responseFormat\"")))
        .andExpect(content().string(containsString("\"mimeType\":\"APPLICATION_JSON\"")))
        .andExpect(content().string(containsString("여행 MBTI: 맛집탐방형")))
        .andExpect(content().string(containsString("최대 3개")))
        .andExpect(content().string(containsString(
            "당일 허용 시간 (Asia/Seoul): 2026-09-01T09:00 ~ 2026-09-02T00:00")))
        .andRespond(withSuccess("""
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"items\\":[{\\"time\\":\\"13:00\\",\\"title\\":\\"카페\\",\\"durationMinutes\\":60,\\"placeId\\":null}]}"
                      }
                    ]
                  }
                }
              ],
              "usageMetadata": {
                "promptTokenCount": 120,
                "cachedContentTokenCount": 20,
                "candidatesTokenCount": 30,
                "thoughtsTokenCount": 10,
                "totalTokenCount": 160
              }
            }
            """, MediaType.APPLICATION_JSON));

    GeminiProperties properties = new GeminiProperties();
    properties.setApiKey("test-key");
    GeminiRestClient client = new GeminiRestClient(builder.build(), properties,
        JsonMapper.builder().build());

    GeminiDto.GenerationResult result = client.generate(input());

    assertThat(result.itinerary().items()).singleElement()
        .extracting(GeminiDto.GeneratedItem::scheduledTime)
        .isEqualTo("13:00");
    assertThat(result.usageMetadata()).isEqualTo(new GeminiDto.UsageMetadata(
        120, 20, 30, 10, 160));
    server.verify();
  }

  @Test
  @DisplayName("후보가 많아도 Gemini 일정 생성 요청은 한 번만 보낸다")
  void generatesItineraryOnceWithManyCandidates() {
    RestClient.Builder builder = RestClient.builder()
        .baseUrl("https://gemini.example.com")
        .defaultHeader("x-goog-api-key", "test-key");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo(
            "https://gemini.example.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
        .andExpect(content().string(containsString("id=1")))
        .andExpect(content().string(containsString("id=2")))
        .andExpect(content().string(containsString("id=3")))
        .andExpect(content().string(containsString("category=ATTRACTION, tags=[OCEAN_VIEW]")))
        .andExpect(content().string(containsString("lat=35.16, lon=129.16")))
        .andExpect(content().string(containsString("숙소 기준점: lat=35.17, lon=129.17")))
        .andRespond(withSuccess(response("{\\\"items\\\":[]}"), MediaType.APPLICATION_JSON));

    GeminiProperties properties = new GeminiProperties();
    properties.setApiKey("test-key");
    GeminiRestClient client = new GeminiRestClient(builder.build(), properties,
        JsonMapper.builder().build());

    GeminiDto.GenerationResult result = client.generate(inputWithCandidates());

    assertThat(result.itinerary().items()).isEmpty();
    assertThat(result.stageUsages()).extracting(GeminiDto.StageUsage::stage)
        .containsExactly("itinerary");
    server.verify();
  }

  private AiItineraryGenerationDto.GenerationInput input() {
    return new AiItineraryGenerationDto.GenerationInput(LocalDate.of(2026, 9, 1),
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        TravelIntensityType.RELAXED, List.of(), TravelMbtiType.FOOD_EXPLORER, List.of(), List.of(), List.of(), null);
  }

  private AiItineraryGenerationDto.GenerationInput inputWithCandidates() {
    return new AiItineraryGenerationDto.GenerationInput(LocalDate.of(2026, 9, 1),
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        TravelIntensityType.RELAXED, List.of(), TravelMbtiType.FOOD_EXPLORER, List.of(), List.of(), List.of(
            new AiItineraryGenerationDto.PlaceCandidate(1L, "장소 1", "부산", PlaceCategory.ATTRACTION,
                List.of(PlaceTag.OCEAN_VIEW), 35.16, 129.16),
            new AiItineraryGenerationDto.PlaceCandidate(2L, "장소 2", "부산", null, List.of(), 35.16, 129.16),
            new AiItineraryGenerationDto.PlaceCandidate(3L, "장소 3", "부산", null, List.of(), 35.16, 129.16)),
        new AiItineraryGenerationDto.AccommodationAnchor(35.17, 129.17));
  }

  private String response(String text) {
    return """
        {
          "candidates": [{"content": {"parts": [{"text": "%s"}]}}],
          "usageMetadata": {
            "promptTokenCount": 120,
            "cachedContentTokenCount": 20,
            "candidatesTokenCount": 30,
            "thoughtsTokenCount": 10,
            "totalTokenCount": 160
          }
        }
        """.formatted(text);
  }
}
