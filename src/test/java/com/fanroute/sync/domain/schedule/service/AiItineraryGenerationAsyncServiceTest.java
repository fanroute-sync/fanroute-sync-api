package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.fanroute.sync.domain.schedule.client.GeminiDto;
import com.fanroute.sync.domain.schedule.client.GeminiRestClient;
import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.entity.TravelIntensityType;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AiItineraryGenerationAsyncServiceTest {

  @Mock
  private AiItineraryGenerationService generationService;
  @Mock
  private GeminiRestClient geminiRestClient;

  @Test
  @DisplayName("선점된 작업은 Gemini 결과를 저장 단계로 전달한다")
  void generatesAndCompletes(CapturedOutput output) {
    AiItineraryGenerationDto.GenerationInput input = input();
    GeminiDto.GeneratedItinerary result = new GeminiDto.GeneratedItinerary(List.of());
    when(generationService.start(10L)).thenReturn(input);
    when(geminiRestClient.generate(input)).thenReturn(generationResult(result));

    AiItineraryGenerationAsyncService.ExecutionResult executionResult = service().generate(10L);

    assertThat(executionResult)
        .isEqualTo(AiItineraryGenerationAsyncService.ExecutionResult.ACKNOWLEDGE);
    verify(generationService).complete(10L, input, result);
    verify(generationService, never()).fail(any());
    assertThat(output).contains("generationId=10", "geminiElapsedMs=",
        "placeCandidateCount=0", "promptTokens=100", "cachedTokens=0",
        "candidateTokens=20", "thoughtsTokens=0", "totalTokens=120");
  }

  @Test
  @DisplayName("Gemini 호출 실패는 재시도 또는 최종 실패 처리 후 ACK한다")
  void handlesGeminiCallFailure(CapturedOutput output) {
    AiItineraryGenerationDto.GenerationInput input = input();
    when(generationService.start(10L)).thenReturn(input);
    when(geminiRestClient.generate(input)).thenThrow(new IllegalStateException());

    AiItineraryGenerationAsyncService.ExecutionResult executionResult = service().generate(10L);

    assertThat(executionResult)
        .isEqualTo(AiItineraryGenerationAsyncService.ExecutionResult.ACKNOWLEDGE);
    verify(generationService).handleGeminiFailure(eq(10L), any(IllegalStateException.class));
    verify(generationService, never()).complete(any(), any(), any());
    assertThat(output).contains("generationId=10", "geminiElapsedMs=",
        "exception=IllegalStateException");
  }

  @Test
  @DisplayName("이미 종료된 작업은 Gemini 호출 없이 ACK한다")
  void acknowledgesTerminalGeneration() {
    when(generationService.start(10L)).thenReturn(null);
    when(generationService.isTerminal(10L)).thenReturn(true);

    AiItineraryGenerationAsyncService.ExecutionResult executionResult = service().generate(10L);

    assertThat(executionResult)
        .isEqualTo(AiItineraryGenerationAsyncService.ExecutionResult.ACKNOWLEDGE);
    verify(geminiRestClient, never()).generate(any());
    verify(generationService, never()).complete(any(), any(), any());
  }

  @Test
  @DisplayName("lease가 유효한 처리 작업은 pending 상태로 남긴다")
  void leavesActiveGenerationPending() {
    when(generationService.start(10L)).thenReturn(null);
    when(generationService.isTerminal(10L)).thenReturn(false);

    AiItineraryGenerationAsyncService.ExecutionResult executionResult = service().generate(10L);

    assertThat(executionResult)
        .isEqualTo(AiItineraryGenerationAsyncService.ExecutionResult.LEAVE_PENDING);
    verify(geminiRestClient, never()).generate(any());
  }

  @Test
  @DisplayName("결과 저장 실패는 실패 상태로 덮지 않고 worker까지 전파한다")
  void propagatesResultPersistenceFailure() {
    AiItineraryGenerationDto.GenerationInput input = input();
    GeminiDto.GeneratedItinerary result = new GeminiDto.GeneratedItinerary(List.of());
    when(generationService.start(10L)).thenReturn(input);
    when(geminiRestClient.generate(input)).thenReturn(generationResult(result));
    org.mockito.Mockito.doThrow(new IllegalStateException("database unavailable"))
        .when(generationService).complete(10L, input, result);

    assertThatThrownBy(() -> service().generate(10L))
        .isInstanceOf(IllegalStateException.class);
    verify(generationService, never()).fail(any());
  }

  @Test
  @DisplayName("검증 불가 Gemini 결과는 실패 확정 후 ACK한다")
  void failsValidationErrorAndAcknowledges() {
    AiItineraryGenerationDto.GenerationInput input = input();
    GeminiDto.GeneratedItinerary result = new GeminiDto.GeneratedItinerary(List.of());
    when(generationService.start(10L)).thenReturn(input);
    when(geminiRestClient.generate(input)).thenReturn(generationResult(result));
    org.mockito.Mockito.doThrow(new BusinessException(ScheduleErrorCode.INVALID_ITINERARY_ITEM))
        .when(generationService).complete(10L, input, result);

    AiItineraryGenerationAsyncService.ExecutionResult executionResult = service().generate(10L);

    assertThat(executionResult)
        .isEqualTo(AiItineraryGenerationAsyncService.ExecutionResult.ACKNOWLEDGE);
    verify(generationService).fail(10L,
        "Validation failed: " + ScheduleErrorCode.INVALID_ITINERARY_ITEM);
  }

  private AiItineraryGenerationAsyncService service() {
    return new AiItineraryGenerationAsyncService(generationService, geminiRestClient);
  }

  private AiItineraryGenerationDto.GenerationInput input() {
    return new AiItineraryGenerationDto.GenerationInput(LocalDate.of(2026, 9, 1),
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        TravelIntensityType.RELAXED, List.of(), List.of(), List.of(), List.of());
  }

  private GeminiDto.GenerationResult generationResult(GeminiDto.GeneratedItinerary itinerary) {
    return new GeminiDto.GenerationResult(itinerary,
        new GeminiDto.UsageMetadata(100, 0, 20, 0, 120));
  }
}
