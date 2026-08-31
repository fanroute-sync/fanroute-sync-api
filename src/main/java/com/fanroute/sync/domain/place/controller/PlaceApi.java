package com.fanroute.sync.domain.place.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.place.dto.PlaceDto;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** {@link PlaceController}의 Swagger 문서 계약. */
@RequestMapping("/api/v1/places")
@Tag(name = "장소", description = "부산 숙박·관광·식음 장소 목록 및 상세 조회 API")
public interface PlaceApi {

  int MAX_PAGE_SIZE = 100;

  @Operation(summary = "장소 목록 조회", description = "TourAPI로 동기화된 장소를 조회합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "장소 목록 조회 성공", useReturnTypeSchema = true)
  })
  @GetMapping
  ResponseEntity<ApiResponse<Page<PlaceDto.Response>>> getPlaces(
      @Parameter(description = "카테고리로 필터링. 생략 시 전체 카테고리 조회")
      @RequestParam(required = false) PlaceCategory category,
      @Parameter(description = "페이지 번호(0부터 시작)")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "페이지 크기(최대 " + MAX_PAGE_SIZE + ")")
      @RequestParam(defaultValue = "20") int size);

  @Operation(summary = "장소 상세 조회")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "장소 상세 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = PlaceErrorCode.class, names = "PLACE_NOT_FOUND")
  @GetMapping("/{placeId}")
  ResponseEntity<ApiResponse<PlaceDto.Response>> getPlace(@PathVariable Long placeId);
}
