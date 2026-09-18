package com.fanroute.sync.domain.concert.dto;

import java.util.Set;

import com.fanroute.sync.domain.concert.entity.ConcertTimeSlot;
import com.fanroute.sync.domain.concert.entity.VenueRecommendedPlace;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.entity.PlaceTag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VenueRecommendedPlaceDto {

  public record CreateRequest(
      @NotNull Long venueId,
      @NotNull Long placeId,
      @NotNull @Min(1) Integer sortOrder,
      @Schema(description = "추천 시간대. null이면 시간대 무관")
      ConcertTimeSlot recommendedTimeSlot) {}

  public record UpdateRequest(
      @NotNull Long placeId,
      @NotNull @Min(1) Integer sortOrder,
      @Schema(description = "추천 시간대. null이면 시간대 무관")
      ConcertTimeSlot recommendedTimeSlot) {}

  public record Response(
      Long id,
      Long venueId,
      Long placeId,
      String placeName,
      PlaceCategory category,
      Set<PlaceTag> tags,
      int sortOrder,
      ConcertTimeSlot recommendedTimeSlot) {

    public static Response from(VenueRecommendedPlace recommendation) {
      return new Response(
          recommendation.getId(),
          recommendation.getVenue().getId(),
          recommendation.getPlace().getId(),
          recommendation.getPlace().getName(),
          recommendation.getPlace().getCategory(),
          recommendation.getPlace().getTags(),
          recommendation.getSortOrder(),
          recommendation.getRecommendedTimeSlot());
    }
  }
}
