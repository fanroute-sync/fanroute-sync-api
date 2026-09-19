package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fanroute.sync.domain.concert.dto.VenuePlaceCollectionDto;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequestMapping("/api/v1/venues/{venueId}/place-collections")
@Tag(name = "공연장 장소 컬렉션", description = "공연장 주변 장소 묶음 조회 API")
public interface VenuePlaceCollectionApi {

  @Operation(summary = "공연장 장소 컬렉션 조회",
      description = "공연장 주변 장소를 운영자가 미리 구성한 컬렉션을 조회합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "컬렉션 조회 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "VENUE_NOT_FOUND")
  @GetMapping
  ResponseEntity<ApiResponse<List<VenuePlaceCollectionDto.Response>>> getCollections(
      @Parameter(description = "공연장 ID") @PathVariable Long venueId);
}
