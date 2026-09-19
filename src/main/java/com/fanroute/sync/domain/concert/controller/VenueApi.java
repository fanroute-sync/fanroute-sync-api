package com.fanroute.sync.domain.concert.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.concert.dto.VenueDto;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** {@link VenueController}의 Swagger 문서 계약. */
@RequestMapping("/api/v1/venues")
@Tag(name = "공연장", description = "공연장 목록 및 상세 조회 API")
public interface VenueApi {

  int MAX_PAGE_SIZE = 100;

  @Operation(summary = "공연장 목록 조회", description = "이름 오름차순으로 공연장 목록을 조회합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "공연장 목록 조회 성공", useReturnTypeSchema = true)
  })
  @GetMapping
  ResponseEntity<ApiResponse<Page<VenueDto.Summary>>> getVenues(
      @Parameter(description = "페이지 번호(0부터 시작)")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "페이지 크기(최대 " + MAX_PAGE_SIZE + ")")
      @RequestParam(defaultValue = "20") int size);

  @Operation(summary = "공연장 상세 조회",
      description = "공연장 기본 정보와 종료되지 않은 공연·추천 데이터 개수를 조회합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "공연장 상세 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "VENUE_NOT_FOUND")
  @GetMapping("/{venueId}")
  ResponseEntity<ApiResponse<VenueDto.Detail>> getVenue(@PathVariable Long venueId);
}
