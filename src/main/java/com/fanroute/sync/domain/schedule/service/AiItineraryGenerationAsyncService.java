package com.fanroute.sync.domain.schedule.service;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.fanroute.sync.domain.schedule.client.GeminiDto;
import com.fanroute.sync.domain.schedule.client.GeminiRestClient;
import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiItineraryGenerationAsyncService {

  public enum ExecutionResult {
    ACKNOWLEDGE,
    LEAVE_PENDING
  }

  private final AiItineraryGenerationService generationService;
  private final GeminiRestClient geminiRestClient;

  public ExecutionResult generate(Long generationId) {
    AiItineraryGenerationDto.GenerationInput input = generationService.start(generationId);
    if (input == null) {
      return generationService.isTerminal(generationId)
          ? ExecutionResult.ACKNOWLEDGE : ExecutionResult.LEAVE_PENDING;
    }

    GeminiDto.GenerationResult generationResult;
    long geminiStartTime = System.nanoTime();
    try {
      generationResult = geminiRestClient.generate(input);
    } catch (RuntimeException exception) {
      log.warn(
          "AI itinerary Gemini call failed: generationId={}, geminiElapsedMs={}, exception={}",
          generationId, elapsedMillis(geminiStartTime), exception.getClass().getSimpleName());
      generationService.handleGeminiFailure(generationId, exception);
      return ExecutionResult.ACKNOWLEDGE;
    }

    GeminiDto.UsageMetadata usage = generationResult.usageMetadata();
    log.info("AI itinerary Gemini call completed: generationId={}, geminiElapsedMs={}, "
            + "placeCandidateCount={}, promptTokens={}, cachedTokens={}, candidateTokens={}, "
            + "thoughtsTokens={}, totalTokens={}",
        generationId, elapsedMillis(geminiStartTime), input.placeCandidates().size(),
        usage == null ? null : usage.promptTokenCount(),
        usage == null ? null : usage.cachedContentTokenCount(),
        usage == null ? null : usage.candidatesTokenCount(),
        usage == null ? null : usage.thoughtsTokenCount(),
        usage == null ? null : usage.totalTokenCount());

    try {
      generationService.complete(generationId, input, generationResult.itinerary());
    } catch (BusinessException exception) {
      log.warn("AI itinerary validation failed: generationId={}, errorCode={}", generationId,
          exception.getErrorCode());
      generationService.fail(generationId, "Validation failed: " + exception.getErrorCode());
    }
    return ExecutionResult.ACKNOWLEDGE;
  }

  private long elapsedMillis(long startTime) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
  }
}
