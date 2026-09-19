package com.fanroute.sync.domain.concert.dto;

import java.util.List;
import java.util.Set;

import com.fanroute.sync.domain.concert.entity.PlaceCollectionType;
import com.fanroute.sync.domain.concert.entity.VenuePlaceCollection;
import com.fanroute.sync.domain.concert.entity.VenuePlaceCollectionItem;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.entity.PlaceTag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VenuePlaceCollectionDto {

  public record ItemRequest(@NotNull Long placeId, @NotNull @Min(1) Integer sortOrder) {}

  public record ImportRequest(
      @NotNull Long venueId,
      @NotBlank String name,
      String description,
      @NotNull PlaceCollectionType type,
      @NotEmpty @Valid List<ItemRequest> items) {}

  public record ItemResponse(Long placeId, String placeName, PlaceCategory category,
      Set<PlaceTag> tags, int sortOrder) {

    public static ItemResponse from(VenuePlaceCollectionItem item) {
      return new ItemResponse(item.getPlace().getId(), item.getPlace().getName(),
          item.getPlace().getCategory(), item.getPlace().getTags(), item.getSortOrder());
    }
  }

  public record Response(Long id, Long venueId, String name, String description,
      PlaceCollectionType type, List<ItemResponse> items) {

    public static Response from(VenuePlaceCollection collection,
        List<VenuePlaceCollectionItem> items) {
      return new Response(collection.getId(), collection.getVenue().getId(), collection.getName(),
          collection.getDescription(), collection.getType(), items.stream().map(ItemResponse::from)
              .toList());
    }
  }
}
