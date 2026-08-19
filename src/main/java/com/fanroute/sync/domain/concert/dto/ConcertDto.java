package com.fanroute.sync.domain.concert.dto;

import java.time.LocalDate;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.Venue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConcertDto {

  public record Response(
      @Schema(description = "공연 ID", example = "1") Long id,
      @Schema(description = "공연명", example = "2026 Fan Route Concert") String title,
      @Schema(description = "장르명", example = "대중음악") String genreName,
      @Schema(description = "공연 시작일") LocalDate startDate,
      @Schema(description = "공연 종료일") LocalDate endDate,
      @Schema(description = "포스터 이미지 URL") String posterUrl,
      @Schema(description = "공연장 정보") VenueSummary venue) {

    public static Response from(Concert concert) {
      return new Response(
          concert.getId(),
          concert.getTitle(),
          concert.getGenreName() == null ? null : concert.getGenreName().label(),
          concert.getStartDate(),
          concert.getEndDate(),
          concert.getPosterUrl(),
          VenueSummary.from(concert.getVenue()));
    }
  }

  public record VenueSummary(
      @Schema(description = "공연장 ID", example = "1") Long id,
      @Schema(description = "공연장명", example = "부산 벡스코 오디토리움") String name,
      @Schema(description = "주소") String address,
      @Schema(description = "위도") Double latitude,
      @Schema(description = "경도") Double longitude) {

    public static VenueSummary from(Venue venue) {
      return new VenueSummary(
          venue.getId(), venue.getName(), venue.getAddress(), venue.getLatitude(),
          venue.getLongitude());
    }
  }
}
