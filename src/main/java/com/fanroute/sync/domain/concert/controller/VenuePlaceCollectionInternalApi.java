package com.fanroute.sync.domain.concert.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.concert.dto.VenuePlaceCollectionDto;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RequestMapping("/api/v1/internal/venue-place-collections")
@Tag(name = "공연장 장소 컬렉션 내부 운영", description = "로컬 운영 도구가 컬렉션을 반영하는 API")
@SecurityRequirement(name = "bearerAuth")
public interface VenuePlaceCollectionInternalApi {

  @Operation(summary = "공연장 장소 컬렉션 반영",
      description = "로컬에서 카테고리별로 선정한 장소 묶음을 원격 서버에 반영합니다. 같은 이름의 컬렉션은 항목을 교체합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "컬렉션 반영 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "VENUE_NOT_FOUND")
  @ApiErrorCodeExamples(type = PlaceErrorCode.class, names = "PLACE_NOT_FOUND")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PostMapping("/import")
  ResponseEntity<ApiResponse<VenuePlaceCollectionDto.Response>> importCollection(
      @Valid @RequestBody VenuePlaceCollectionDto.ImportRequest request);
}
