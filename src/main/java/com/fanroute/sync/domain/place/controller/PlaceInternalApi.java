package com.fanroute.sync.domain.place.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.place.dto.PlaceDto;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.global.batch.JobRunResult;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RequestMapping("/api/v1/internal/places")
@Tag(name = "장소 내부 운영", description = "로컬 운영 도구가 호출하는 장소 수집·후보 API")
@SecurityRequirement(name = "bearerAuth")
public interface PlaceInternalApi {

  @Operation(summary = "장소 동기화 실행", description = "로컬 운영 도구에서 TourAPI 동기화 Job을 실행합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "동기화 실행 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = PlaceErrorCode.class, names = "SYNC_ALREADY_RUNNING")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PostMapping("/sync")
  ResponseEntity<ApiResponse<List<JobRunResult>>> syncPlaces(
      @Parameter(description = "동기화할 카테고리")
      @RequestParam(required = false) PlaceCategory category);

  @Operation(summary = "주변 장소 후보 조회", description = "로컬 운영 도구에서 TourAPI 후보를 조회합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "후보 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class,
      names = {"VENUE_NOT_FOUND", "VENUE_LOCATION_NOT_FOUND"})
  @ApiErrorCodeExamples(type = PlaceErrorCode.class, names = "TOUR_API_RESPONSE_INVALID")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @GetMapping("/nearby-candidates")
  ResponseEntity<ApiResponse<List<PlaceDto.NearbyCandidateResponse>>> getNearbyCandidates(
      @Parameter(description = "공연장 ID")
      @RequestParam Long venueId,
      @RequestParam PlaceCategory category,
      @RequestParam(defaultValue = "1000") int radiusMeters);

  @Operation(summary = "장소 후보 벌크 반영",
      description = "로컬에서 검수한 TourAPI 후보를 contentId 기준으로 장소에 upsert하고 placeId를 반환합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "장소 반영 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PostMapping("/import")
  ResponseEntity<ApiResponse<List<PlaceDto.Response>>> importCandidates(
      @Valid @RequestBody PlaceDto.CandidateImportBatchRequest request);
}
