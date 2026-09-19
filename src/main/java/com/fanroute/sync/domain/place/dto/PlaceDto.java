package com.fanroute.sync.domain.place.dto;

import java.util.List;
import java.util.Set;

import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.entity.PlaceTag;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlaceDto {

  public record CandidateImportRequest(
      @NotBlank String contentId,
      @NotBlank String name,
      String address,
      String detailAddress,
      String zipCode,
      Double latitude,
      Double longitude,
      String telephone,
      String imageUrl,
      String thumbnailUrl,
      String copyrightType) {}

  public record CandidateImportBatchRequest(
      @NotNull PlaceCategory category,
      @NotEmpty @Valid List<CandidateImportRequest> candidates) {}

  public record UpdateTagsRequest(
      @Schema(description = "장소 태그", example = "[\"CAFE\", \"OCEAN_VIEW\"]")
      @NotNull Set<PlaceTag> tags) {}

  public record NearbyCandidateResponse(
      @Schema(description = "TourAPI 콘텐츠 ID", example = "2868824") String contentId,
      @Schema(description = "장소명", example = "비스포레") String name,
      @Schema(description = "주소") String address,
      @Schema(description = "위도") Double latitude,
      @Schema(description = "경도") Double longitude,
      @Schema(description = "검색 기준 좌표로부터의 거리(m)", example = "910.87") Double distanceMeters,
      @Schema(description = "대표 이미지 URL") String imageUrl,
      @Schema(
          description = "이미 TourAPI 동기화로 저장된 장소면 그 ID. 아직 동기화 전이면 null이며, "
              + "그 경우 추천 장소로 바로 등록할 수 없고 동기화를 먼저 기다려야 함",
          example = "1") Long existingPlaceId) {

    public static NearbyCandidateResponse from(
        TourApiDto.PlaceSummary summary, Long existingPlaceId) {
      return new NearbyCandidateResponse(
          summary.contentId(),
          summary.name(),
          summary.address(),
          summary.latitude(),
          summary.longitude(),
          summary.distanceMeters(),
          summary.imageUrl(),
          existingPlaceId);
    }
  }

  public record Response(
      @Schema(description = "장소 ID", example = "1") Long id,
      @Schema(description = "카테고리") PlaceCategory category,
      @Schema(description = "장소명", example = "부산 해운대 호텔") String name,
      @Schema(description = "주소") String address,
      @Schema(description = "상세 주소") String detailAddress,
      @Schema(description = "우편번호") String zipCode,
      @Schema(description = "위도") Double latitude,
      @Schema(description = "경도") Double longitude,
      @Schema(description = "대표 이미지 URL") String imageUrl,
      @Schema(description = "썸네일 이미지 URL") String thumbnailUrl,
      @Schema(
          description = "이미지 저작권 유형(공공누리). null이면 확인되지 않은 것이므로 임의로 자유이용으로 "
              + "간주하면 안 됨", example = "Type3")
      String copyrightType,
      @Schema(description = "데이터 출처. 이미지 사용 시 출처 표시에 사용", example = "KTO_TOUR_API")
      String source,
      @Schema(description = "전화번호") String telephone,
      @Schema(description = "장소 자체의 특성 태그") Set<PlaceTag> tags) {

    public static Response from(Place place) {
      return new Response(
          place.getId(),
          place.getCategory(),
          place.getName(),
          place.getAddress(),
          place.getDetailAddress(),
          place.getZipCode(),
          place.getLatitude(),
          place.getLongitude(),
          place.getImageUrl(),
          place.getThumbnailUrl(),
          place.getCopyrightType(),
          place.getSource(),
          place.getTelephone(),
          place.getTags());
    }
  }
}
