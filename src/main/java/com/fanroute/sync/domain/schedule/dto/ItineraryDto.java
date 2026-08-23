package com.fanroute.sync.domain.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fanroute.sync.domain.schedule.entity.ItineraryItemType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ItineraryDto {
  private ItineraryDto() {}

  public record CreateItemRequest(@NotNull ItineraryItemType type, LocalTime scheduledTime,
      @NotBlank String title, Integer durationMinutes, Long placeId) {}
  public record UpdateItemRequest(LocalTime scheduledTime, @NotBlank String title,
      Integer durationMinutes, Long placeId) {}
  public record ReorderRequest(@NotNull List<Long> itemIds) {}
  public record ItemResponse(Long id, int sortOrder, LocalTime scheduledTime, ItineraryItemType type,
      String title, Integer durationMinutes, Long placeId, Long concertId, boolean fixed) {}
  public record DayResponse(Long itineraryDayId, LocalDate date, boolean concertDay,
      List<ItemResponse> items) {}
}
