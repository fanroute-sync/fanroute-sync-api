package com.fanroute.sync.domain.concert.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.fanroute.sync.domain.concert.entity.VenueItineraryTemplate;
import com.fanroute.sync.domain.concert.entity.VenueItineraryTemplateItem;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.entity.PlaceTag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VenueItineraryTemplateDto {

  public record ItemRequest(
      @NotNull Long placeId,
      @NotNull @Min(1) Integer sortOrder,
      LocalTime defaultTime,
      @Min(1) Integer defaultDurationMinutes) {}

  public record ImportRequest(
      @NotNull Long venueId,
      @NotBlank String name,
      String description,
      @NotEmpty @Size(max = 5) @Valid @Schema(description = "템플릿에 포함할 장소 항목. 최대 5개")
      List<ItemRequest> items) {}

  public record ItemResponse(Long placeId, String placeName, PlaceCategory category,
      Set<PlaceTag> tags, int sortOrder, LocalTime defaultTime,
      Integer defaultDurationMinutes) {

    public static ItemResponse from(VenueItineraryTemplateItem item) {
      return new ItemResponse(item.getPlace().getId(), item.getPlace().getName(),
          item.getPlace().getCategory(), item.getPlace().getTags(), item.getSortOrder(),
          item.getDefaultTime(), item.getDefaultDurationMinutes());
    }
  }

  public record Response(Long id, Long venueId, String name, String description,
      List<ItemResponse> items) {

    public static Response from(VenueItineraryTemplate template,
        List<VenueItineraryTemplateItem> items) {
      return new Response(template.getId(), template.getVenue().getId(), template.getName(),
          template.getDescription(), items.stream().map(ItemResponse::from).toList());
    }
  }
}
