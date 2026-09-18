package com.fanroute.sync.domain.concert.dto;

import com.fanroute.sync.domain.concert.entity.Venue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VenueDto {

  public record Summary(
      @Schema(description = "공연장 ID", example = "1") Long id,
      @Schema(description = "KOPIS 공연시설 ID", example = "FC000001") String kopisVenueId,
      @Schema(description = "공연장명", example = "부산 벡스코 오디토리움") String name,
      @Schema(description = "주소") String address,
      @Schema(description = "위도") Double latitude,
      @Schema(description = "경도") Double longitude) {

    public static Summary from(Venue venue) {
      return new Summary(venue.getId(), venue.getKopisVenueId(), venue.getName(),
          venue.getAddress(), venue.getLatitude(), venue.getLongitude());
    }
  }

  public record Detail(
      Summary venue,
      @Schema(description = "종료되지 않은 공연 수", example = "3") long upcomingConcertCount,
      @Schema(description = "공연장 장소 컬렉션 수", example = "2") long placeCollectionCount) {

    public static Detail from(Venue venue, long upcomingConcertCount, long placeCollectionCount) {
      return new Detail(Summary.from(venue), upcomingConcertCount, placeCollectionCount);
    }
  }
}
