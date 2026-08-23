package com.fanroute.sync.domain.schedule.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fanroute.sync.domain.schedule.entity.TravelTimeSlot;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public final class TripPlanDto {

  private TripPlanDto() {
  }

  public record CreateRequest(
      @NotNull @Schema(example = "2026-09-01") LocalDate arrivalDate,
      @NotNull @Schema(example = "MORNING") TravelTimeSlot arrivalTimeSlot,
      @NotNull @Schema(example = "2026-09-03") LocalDate departureDate,
      @NotNull @Schema(example = "EVENING") TravelTimeSlot departureTimeSlot,
      @Schema(example = "1", nullable = true) Long concertId) {
  }

  public record CreateResponse(Long tripPlanId, Long concertId, Instant arrivalAt,
      Instant departureAt, List<ItineraryDayResponse> itineraryDays) {
  }

  public record ItineraryDayResponse(Long id, LocalDate date, boolean concertDay) {
  }

  public record SummaryResponse(Long tripPlanId, Long concertId, String concertTitle,
      Instant arrivalAt, Instant departureAt) {
  }

  public record DetailResponse(Long tripPlanId, Long concertId, String concertTitle,
      Instant arrivalAt, Instant departureAt, List<ItineraryDayResponse> itineraryDays) {
  }
}
