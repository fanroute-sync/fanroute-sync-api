package com.fanroute.sync.domain.schedule.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fanroute.sync.domain.schedule.entity.TravelTimeSlot;
import com.fanroute.sync.domain.schedule.entity.TravelIntensityType;
import com.fanroute.sync.domain.schedule.entity.CompanionType;
import com.fanroute.sync.domain.schedule.entity.TravelMbtiType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

  public record CreateResponse(Long tripPlanId, Long concertId,
      Instant arrivalAt, Instant departureAt, List<ItineraryDayResponse> itineraryDays) {
  }

  public record ItineraryDayResponse(Long id, LocalDate date, boolean concertDay) {
  }

  public record SummaryResponse(Long tripPlanId, Long concertId, String concertTitle,
      Instant arrivalAt, Instant departureAt) {
  }

  public record DetailResponse(Long tripPlanId, Long concertId, String concertTitle,
      Instant arrivalAt, Instant departureAt,
      List<ItineraryDayResponse> itineraryDays) {
  }

  public record CreateAccommodationRequest(
      @Schema(description = "TourAPI 숙소 장소 ID. 있으면 해당 장소 좌표를 사용합니다.") Long placeId,
      @Schema(description = "직접 입력한 숙소명 또는 주소. placeId가 없을 때 필수입니다.", example = "부산 해운대구 해운대해변로 296")
      String nameOrAddress,
      @NotNull @Schema(example = "2026-09-19") LocalDate checkinDate,
      @NotNull @Schema(example = "2026-09-21") LocalDate checkoutDate) {

    @AssertTrue(message = "placeId 또는 nameOrAddress 중 하나는 필요합니다.")
    public boolean hasPlaceOrName() {
      return placeId != null || (nameOrAddress != null && !nameOrAddress.isBlank());
    }
  }

  public record AccommodationResponse(Long accommodationId, Long placeId, String nameOrAddress,
      Double latitude, Double longitude, LocalDate checkinDate, LocalDate checkoutDate) {
  }

  public record ReplaceAccommodationsRequest(
      @NotNull @Size(max = 10) List<@Valid CreateAccommodationRequest> accommodations) {
  }

  public record UpdateTravelStyleRequest(
      @NotNull TravelIntensityType travelIntensity,
      @NotNull @Size(min = 1, max = 10)
      @Schema(description = "복수 선택 동행", allowableValues = {"친구", "연인", "혼자", "부모님", "아이"})
      List<CompanionType> companions,
      @NotNull
      @Schema(description = "여행 MBTI", allowableValues = {
          "맛집탐방형", "감성사진형", "역사문화형", "자연힐링형", "쇼핑집중형", "카페투어형", "액티비티형", "로컬체험형", "공연몰입형"})
      TravelMbtiType travelMbti) {
  }

  public record TravelStyleResponse(TravelIntensityType travelIntensity, List<CompanionType> companions,
      TravelMbtiType travelMbti) {
  }
}
