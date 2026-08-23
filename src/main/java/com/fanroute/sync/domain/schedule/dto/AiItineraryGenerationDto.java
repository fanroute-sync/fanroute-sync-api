package com.fanroute.sync.domain.schedule.dto;

import java.time.LocalDate;

import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;

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
}
