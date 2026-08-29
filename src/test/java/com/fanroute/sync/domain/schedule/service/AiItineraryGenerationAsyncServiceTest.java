package com.fanroute.sync.domain.schedule.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fanroute.sync.domain.schedule.client.GeminiDto;
import com.fanroute.sync.domain.schedule.client.GeminiRestClient;
import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.entity.TravelIntensityType;

@ExtendWith(MockitoExtension.class)
class AiItineraryGenerationAsyncServiceTest {

  @Mock
  private AiItineraryGenerationService generationService;
  @Mock
  private GeminiRestClient geminiRestClient;

  @Test
  @DisplayName("선점된 작업은 Gemini 결과를 저장 단계로 전달한다")
  void generatesAndCompletes() {
    AiItineraryGenerationDto.GenerationInput input = input();
    GeminiDto.GeneratedItinerary result = new GeminiDto.GeneratedItinerary(List.of());
    when(generationService.start(10L)).thenReturn(input);
    when(geminiRestClient.generate(input)).thenReturn(result);

    service().generate(10L);

    verify(generationService).complete(10L, input, result);
    verify(generationService, never()).fail(any());
  }

  @Test
  @DisplayName("Gemini 호출 실패는 작업을 실패 상태로 처리한다")
  void failsWhenGeminiCallFails() {
    AiItineraryGenerationDto.GenerationInput input = input();
    when(generationService.start(10L)).thenReturn(input);
    when(geminiRestClient.generate(input)).thenThrow(new IllegalStateException());

    service().generate(10L);

    verify(generationService).fail(10L);
    verify(generationService, never()).complete(any(), any(), any());
  }

  @Test
  @DisplayName("이미 취소되었거나 선점된 작업은 Gemini를 호출하지 않는다")
  void skipsUnclaimableGeneration() {
    when(generationService.start(10L)).thenReturn(null);

    service().generate(10L);

    verify(geminiRestClient, never()).generate(any());
    verify(generationService, never()).complete(any(), any(), any());
  }

  private AiItineraryGenerationAsyncService service() {
    return new AiItineraryGenerationAsyncService(generationService, geminiRestClient);
  }

  private AiItineraryGenerationDto.GenerationInput input() {
    return new AiItineraryGenerationDto.GenerationInput(LocalDate.of(2026, 9, 1),
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        TravelIntensityType.RELAXED, List.of(), List.of(), List.of(), List.of());
  }
}
