package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.RecommendationTemplateDto;
import com.fanroute.sync.domain.concert.service.RecommendationTemplateService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RecommendationTemplateAdminController implements RecommendationTemplateAdminApi {

  private final RecommendationTemplateService service;

  @Override
  public ResponseEntity<ApiResponse<RecommendationTemplateDto.Response>> create(
      RecommendationTemplateDto.CreateTemplateRequest request) {
    var template = service.create(request);
    return ApiResponse.ok(RecommendationTemplateDto.Response.from(template, List.of()))
        .toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<RecommendationTemplateDto.Response>> update(
      Long templateId, RecommendationTemplateDto.UpdateTemplateRequest request) {
    var template = service.update(templateId, request);
    return ApiResponse.ok(RecommendationTemplateDto.Response.from(
        template, service.getPlaces(templateId))).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> delete(Long templateId) {
    service.delete(templateId);
    return ApiResponse.<Void>ok().toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<RecommendationTemplateDto.PlaceResponse>> addPlace(
      Long templateId, RecommendationTemplateDto.AddPlaceRequest request) {
    return ApiResponse.ok(RecommendationTemplateDto.PlaceResponse.from(
        service.addPlace(templateId, request))).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> removePlace(Long templatePlaceId) {
    service.removePlace(templatePlaceId);
    return ApiResponse.<Void>ok().toResponseEntity();
  }
}
