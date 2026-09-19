package com.fanroute.sync.domain.schedule.dto;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;
import com.fanroute.sync.domain.schedule.entity.TravelIntensityType;
import com.fanroute.sync.domain.schedule.entity.CompanionType;
import com.fanroute.sync.domain.schedule.entity.TravelMbtiType;

import io.swagger.v3.oas.annotations.media.Schema;

public final class AiItineraryGenerationDto {

  private AiItineraryGenerationDto() {
  }

  public record CreateResponse(
      @Schema(description = "AI 일정 생성 작업 ID", example = "1") Long generationId,
      @Schema(description = "생성 작업 상태", example = "PENDING")
      AiItineraryGenerationStatus status) {

    public static CreateResponse from(AiItineraryGeneration generation) {
      return new CreateResponse(generation.getId(), generation.getStatus());
    }
  }

  public record StatusResponse(
      @Schema(description = "AI 일정 생성 작업 ID", example = "1") Long generationId,
      @Schema(description = "대상 날짜별 일정 ID", example = "3") Long itineraryDayId,
      @Schema(description = "대상 날짜", example = "2026-09-01") LocalDate date,
      @Schema(description = "생성 작업 상태", example = "PENDING")
      AiItineraryGenerationStatus status) {

    public static StatusResponse from(AiItineraryGeneration generation) {
      return new StatusResponse(generation.getId(), generation.getItineraryDay().getId(),
          generation.getItineraryDay().getDate(), generation.getStatus());
    }
  }

  public record GenerationInput(
      LocalDate date,
      Instant arrivalAt,
      Instant departureAt,
      TravelIntensityType travelIntensity,
      List<CompanionType> companions,
      TravelMbtiType travelMbti,
      List<String> preferences,
      List<FixedItem> fixedItems,
      List<PlaceCandidate> placeCandidates) {
  }

  public record FixedItem(LocalTime scheduledTime, String title, Integer durationMinutes) {
  }

  public record PlaceCandidate(Long id, String name, String address) {
  }
}
