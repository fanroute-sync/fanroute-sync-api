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
    long processingStartTime = System.nanoTime();
    long preparationElapsedMillis = 0;
    long geminiElapsedMillis = 0;
    Long persistenceElapsedMillis = null;
    String outcome = "PREPARATION_FAILED";
    AiItineraryGenerationDto.GenerationInput input = null;
    try {
      try {
        input = generationService.start(generationId);
      } catch (RuntimeException exception) {
        preparationElapsedMillis = elapsedMillis(processingStartTime);
        throw exception;
      }
      if (input == null) {
        return generationService.isTerminal(generationId)
            ? ExecutionResult.ACKNOWLEDGE : ExecutionResult.LEAVE_PENDING;
      }
      preparationElapsedMillis = elapsedMillis(processingStartTime);
      log.info("AI itinerary input prepared: generationId={}, preparationElapsedMs={}",
          generationId, preparationElapsedMillis);

      GeminiDto.GenerationResult generationResult;
      long geminiStartTime = System.nanoTime();
      try {
        generationResult = geminiRestClient.generate(input);
      } catch (RuntimeException exception) {
        geminiElapsedMillis = elapsedMillis(geminiStartTime);
        outcome = "MODEL_FAILED";
        log.warn(
            "AI itinerary Gemini call failed: generationId={}, geminiElapsedMs={}, exception={}",
            generationId, geminiElapsedMillis, exception.getClass().getSimpleName());
        generationService.handleGeminiFailure(generationId, exception);
        return ExecutionResult.ACKNOWLEDGE;
      }
      geminiElapsedMillis = elapsedMillis(geminiStartTime);

      GeminiDto.UsageMetadata usage = generationResult.usageMetadata();
      generationResult.stageUsages().forEach(stageUsage -> {
        GeminiDto.UsageMetadata stageUsageMetadata = stageUsage.usageMetadata();
        log.info("AI itinerary Gemini stage completed: generationId={}, stage={}, promptTokens={}, "
                + "cachedTokens={}, candidateTokens={}, thoughtsTokens={}, totalTokens={}",
            generationId, stageUsage.stage(),
            stageUsageMetadata == null ? null : stageUsageMetadata.promptTokenCount(),
            stageUsageMetadata == null ? null : stageUsageMetadata.cachedContentTokenCount(),
            stageUsageMetadata == null ? null : stageUsageMetadata.candidatesTokenCount(),
            stageUsageMetadata == null ? null : stageUsageMetadata.thoughtsTokenCount(),
            stageUsageMetadata == null ? null : stageUsageMetadata.totalTokenCount());
      });
      log.info("AI itinerary Gemini call completed: generationId={}, geminiElapsedMs={}, "
              + "placeCandidateCount={}, promptTokens={}, cachedTokens={}, candidateTokens={}, "
              + "thoughtsTokens={}, totalTokens={}",
          generationId, geminiElapsedMillis, input.placeCandidates().size(),
          usage == null ? null : usage.promptTokenCount(),
          usage == null ? null : usage.cachedContentTokenCount(),
          usage == null ? null : usage.candidatesTokenCount(),
          usage == null ? null : usage.thoughtsTokenCount(),
          usage == null ? null : usage.totalTokenCount());

      long persistenceStartTime = System.nanoTime();
      outcome = "PERSISTENCE_FAILED";
      try {
        generationService.complete(generationId, input, generationResult.itinerary());
        outcome = "COMPLETED";
      } catch (BusinessException exception) {
        outcome = "VALIDATION_FAILED";
        log.warn("AI itinerary validation failed: generationId={}, errorCode={}", generationId,
            exception.getErrorCode());
        generationService.fail(generationId, "Validation failed: " + exception.getErrorCode());
      } finally {
        persistenceElapsedMillis = elapsedMillis(persistenceStartTime);
      }
      return ExecutionResult.ACKNOWLEDGE;
    } finally {
      if (input != null || "PREPARATION_FAILED".equals(outcome)) {
        log.info("AI itinerary processing finished: generationId={}, outcome={}, "
                + "preparationElapsedMs={}, geminiElapsedMs={}, persistenceElapsedMs={}, "
                + "processingElapsedMs={}",
            generationId, outcome, preparationElapsedMillis, geminiElapsedMillis,
            persistenceElapsedMillis, elapsedMillis(processingStartTime));
      }
    }
  }

  private long elapsedMillis(long startTime) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
  }
}
