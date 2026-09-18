package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.concert.dto.VenueRecommendedPlaceDto;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RequestMapping("/api/v1/admin/venue-recommended-places")
@Tag(name = "공연장 추천 장소 관리", description = "관리자 전용 공연장 추천 장소 관리 API")
@SecurityRequirement(name = "bearerAuth")
public interface VenueRecommendedPlaceAdminApi {

  @Operation(summary = "공연장 추천 장소 벌크 임포트",
      description = "로컬에서 직접 만든 추천 장소 목록을 한 번에 등록합니다. 서버는 AI를 호출하지 "
          + "않고 전달받은 값을 단건 등록과 같은 검증 경로로 저장하며, 하나라도 실패하면 전체가 "
          + "롤백됩니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "임포트 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class,
      names = {"VENUE_NOT_FOUND", "DUPLICATE_RECOMMENDED_PLACE"})
  @ApiErrorCodeExamples(type = PlaceErrorCode.class, names = "PLACE_NOT_FOUND")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PostMapping("/import")
  ResponseEntity<ApiResponse<List<VenueRecommendedPlaceDto.Response>>> importRecommendations(
      @Valid @RequestBody VenueRecommendedPlaceDto.BulkImportRequest request);

  @Operation(summary = "공연장 추천 장소 등록")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "추천 장소 등록 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class,
      names = {"VENUE_NOT_FOUND", "DUPLICATE_RECOMMENDED_PLACE"})
  @ApiErrorCodeExamples(type = PlaceErrorCode.class, names = "PLACE_NOT_FOUND")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PostMapping
  ResponseEntity<ApiResponse<VenueRecommendedPlaceDto.Response>> create(
      @Valid @RequestBody VenueRecommendedPlaceDto.CreateRequest request);

  @Operation(summary = "공연장 추천 장소 수정")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "추천 장소 수정 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class,
      names = {"RECOMMENDED_PLACE_NOT_FOUND", "DUPLICATE_RECOMMENDED_PLACE"})
  @ApiErrorCodeExamples(type = PlaceErrorCode.class, names = "PLACE_NOT_FOUND")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PutMapping("/{recommendationId}")
  ResponseEntity<ApiResponse<VenueRecommendedPlaceDto.Response>> update(
      @PathVariable Long recommendationId,
      @Valid @RequestBody VenueRecommendedPlaceDto.UpdateRequest request);

  @Operation(summary = "공연장 추천 장소 삭제")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "추천 장소 삭제 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "RECOMMENDED_PLACE_NOT_FOUND")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @DeleteMapping("/{recommendationId}")
  ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long recommendationId);
}
