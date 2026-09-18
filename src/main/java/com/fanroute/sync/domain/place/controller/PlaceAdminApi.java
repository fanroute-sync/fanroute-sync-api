package com.fanroute.sync.domain.place.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
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

/** {@link PlaceAdminController}의 Swagger 문서 계약. */
@RequestMapping("/api/v1/admin/places")
@Tag(name = "장소 관리", description = "관리자 전용 TourAPI 동기화 API")
@SecurityRequirement(name = "bearerAuth")
public interface PlaceAdminApi {

  @Operation(
      summary = "TourAPI 장소 수동 동기화",
      description = "ADMIN 권한을 가진 사용자만 TourAPI 장소 동기화 Job을 즉시 실행합니다. "
          + "category를 지정하면 해당 Job만, 생략하면 카테고리별 Job을 격리된 상태로 모두 실행합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "동기화 실행 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = PlaceErrorCode.class,
      names = {"SYNC_ALREADY_RUNNING"})
  @ApiErrorCodeExamples(
      type = AuthErrorCode.class,
      names = {"ACCESS_DENIED"})
  @PostMapping("/sync")
  ResponseEntity<ApiResponse<List<JobRunResult>>> syncPlaces(
      @Parameter(description = "동기화할 카테고리. 생략 시 전체 카테고리를 격리된 상태로 동기화")
      @RequestParam(required = false) PlaceCategory category);

  @Operation(
      summary = "좌표 주변 인기 장소 후보 조회",
      description = "TourAPI를 조회순으로 실시간 호출해 좌표 반경 안의 후보를 보여줍니다. "
          + "공연장 추천 장소 등록 전에 참고용으로 쓰는 큐레이션 보조 API이며 결과를 저장하지 않습니다. "
          + "existingPlaceId가 null이면 아직 TourAPI 동기화 전이라 추천 장소로 바로 연결할 수 없습니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "후보 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = PlaceErrorCode.class, names = "TOUR_API_RESPONSE_INVALID")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @GetMapping("/nearby-candidates")
  ResponseEntity<ApiResponse<List<PlaceDto.NearbyCandidateResponse>>> getNearbyCandidates(
      @Parameter(description = "카테고리") @RequestParam PlaceCategory category,
      @Parameter(description = "위도") @RequestParam double latitude,
      @Parameter(description = "경도") @RequestParam double longitude,
      @Parameter(description = "검색 반경(m)") @RequestParam(defaultValue = "1000") int radiusMeters);

  @Operation(summary = "장소 태그 수정", description = "장소 자체의 특성을 허용된 태그 목록으로 교체합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "장소 태그 수정 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = PlaceErrorCode.class, names = "PLACE_NOT_FOUND")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PutMapping("/{placeId}/tags")
  ResponseEntity<ApiResponse<PlaceDto.Response>> updateTags(
      @PathVariable Long placeId, @Valid @RequestBody PlaceDto.UpdateTagsRequest request);
}
