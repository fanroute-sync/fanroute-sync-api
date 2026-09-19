package com.fanroute.sync.domain.concert.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.concert.dto.VenueItineraryTemplateDto;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RequestMapping("/api/v1/internal/venue-itinerary-templates")
@Tag(name = "공연장 일정 템플릿 내부 운영", description = "로컬 Gemini 결과를 템플릿으로 반영하는 API")
@SecurityRequirement(name = "bearerAuth")
public interface VenueItineraryTemplateInternalApi {

  @Operation(summary = "공연장 일정 템플릿 반영",
      description = "로컬에서 생성한 공연장별 장소 일정 초안을 저장합니다. 같은 이름은 항목을 교체합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "템플릿 반영 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "VENUE_NOT_FOUND")
  @ApiErrorCodeExamples(type = PlaceErrorCode.class, names = "PLACE_NOT_FOUND")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PostMapping("/import")
  ResponseEntity<ApiResponse<VenueItineraryTemplateDto.Response>> importTemplate(
      @Valid @RequestBody VenueItineraryTemplateDto.ImportRequest request);
}
