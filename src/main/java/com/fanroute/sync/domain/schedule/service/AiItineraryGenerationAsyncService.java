package com.fanroute.sync.domain.schedule.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fanroute.sync.domain.schedule.client.GeminiDto;
import com.fanroute.sync.domain.schedule.client.GeminiRestClient;
import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiItineraryGenerationAsyncService {

  private final AiItineraryGenerationService generationService;
  private final GeminiRestClient geminiRestClient;

  @Async("aiItineraryGenerationExecutor")
  public void generate(Long generationId) {
    try {
      AiItineraryGenerationDto.GenerationInput input = generationService.start(generationId);
      if (input == null) {
        return;
      }
      GeminiDto.GeneratedItinerary generatedItinerary = geminiRestClient.generate(input);
      generationService.complete(generationId, input, generatedItinerary);
    } catch (Exception exception) {
      log.warn("AI itinerary generation failed: generationId={}, exception={}", generationId,
          exception.getClass().getSimpleName());
      generationService.fail(generationId);
    }
  }
}
