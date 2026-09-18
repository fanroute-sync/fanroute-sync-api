package com.fanroute.sync.domain.concert.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.concert.dto.RecommendationTemplateDto;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RequestMapping("/api/v1/admin/recommendation-templates")
@Tag(name = "추천 코스 관리", description = "관리자 전용 추천 코스 관리 API")
@SecurityRequirement(name = "bearerAuth")
public interface RecommendationTemplateAdminApi {

  @Operation(summary = "추천 코스 생성")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "추천 코스 생성 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "VENUE_NOT_FOUND")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PostMapping
  ResponseEntity<ApiResponse<RecommendationTemplateDto.Response>> create(
      @Valid @RequestBody RecommendationTemplateDto.CreateTemplateRequest request);

  @Operation(summary = "추천 코스 수정")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "추천 코스 수정 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class,
      names = "RECOMMENDATION_TEMPLATE_NOT_FOUND")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PutMapping("/{templateId}")
  ResponseEntity<ApiResponse<RecommendationTemplateDto.Response>> update(
      @PathVariable Long templateId,
      @Valid @RequestBody RecommendationTemplateDto.UpdateTemplateRequest request);

  @Operation(summary = "추천 코스 삭제")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "추천 코스 삭제 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class,
      names = "RECOMMENDATION_TEMPLATE_NOT_FOUND")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @DeleteMapping("/{templateId}")
  ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long templateId);

  @Operation(summary = "추천 코스에 장소 추가")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "추천 코스 장소 추가 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class,
      names = {"RECOMMENDATION_TEMPLATE_NOT_FOUND", "RECOMMENDED_PLACE_NOT_FOUND",
          "RECOMMENDED_PLACE_VENUE_MISMATCH", "DUPLICATE_RECOMMENDATION_TEMPLATE_PLACE"})
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PostMapping("/{templateId}/places")
  ResponseEntity<ApiResponse<RecommendationTemplateDto.PlaceResponse>> addPlace(
      @PathVariable Long templateId,
      @Valid @RequestBody RecommendationTemplateDto.AddPlaceRequest request);

  @Operation(summary = "추천 코스에서 장소 제거")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "추천 코스 장소 제거 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class,
      names = "RECOMMENDATION_TEMPLATE_PLACE_NOT_FOUND")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @DeleteMapping("/places/{templatePlaceId}")
  ResponseEntity<ApiResponse<Void>> removePlace(@PathVariable Long templatePlaceId);
}
