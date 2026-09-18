package com.fanroute.sync.domain.concert.dto;

import java.util.List;
import java.util.Set;

import com.fanroute.sync.domain.concert.entity.ConcertTimeSlot;
import com.fanroute.sync.domain.concert.entity.RecommendationTemplate;
import com.fanroute.sync.domain.concert.entity.RecommendationTemplatePlace;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.entity.PlaceTag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecommendationTemplateDto {

  public record CreateTemplateRequest(
      @NotNull Long venueId, @NotBlank String name, String description) {}

  public record UpdateTemplateRequest(@NotBlank String name, String description) {}

  public record AddPlaceRequest(
      @NotNull Long recommendedPlaceId, @NotNull @Min(1) Integer sortOrder) {}

  public record PlaceResponse(
      Long id, int sortOrder, Long recommendedPlaceId, Long placeId, String placeName,
      PlaceCategory category, Set<PlaceTag> tags, ConcertTimeSlot recommendedTimeSlot) {

    public static PlaceResponse from(RecommendationTemplatePlace item) {
      return new PlaceResponse(
          item.getId(), item.getSortOrder(), item.getRecommendedPlace().getId(),
          item.getRecommendedPlace().getPlace().getId(), item.getRecommendedPlace().getPlace().getName(),
          item.getRecommendedPlace().getPlace().getCategory(),
          item.getRecommendedPlace().getPlace().getTags(),
          item.getRecommendedPlace().getRecommendedTimeSlot());
    }
  }

  public record Response(Long id, Long venueId, String name, String description,
      List<PlaceResponse> places) {

    public static Response from(RecommendationTemplate template,
        List<RecommendationTemplatePlace> places) {
      return new Response(template.getId(), template.getVenue().getId(), template.getName(),
          template.getDescription(), places.stream().map(PlaceResponse::from).toList());
    }
  }
}
