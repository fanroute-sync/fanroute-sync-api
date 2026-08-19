package com.fanroute.sync.domain.place.dto;

import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlaceDto {

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
      @Schema(description = "전화번호") String telephone) {

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
          place.getTelephone());
    }
  }
}
