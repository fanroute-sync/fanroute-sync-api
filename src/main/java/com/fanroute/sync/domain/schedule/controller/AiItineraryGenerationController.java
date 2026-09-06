package com.fanroute.sync.domain.schedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.service.AiItineraryGenerationService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.response.SuccessCode;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AiItineraryGenerationController implements AiItineraryGenerationApi {

  private final CurrentUserResolver currentUserResolver;
  private final AiItineraryGenerationService generationService;

  @Override
  public ResponseEntity<ApiResponse<AiItineraryGenerationDto.CreateResponse>> requestGeneration(
      Jwt jwt, Long itineraryDayId) {
    User user = currentUserResolver.getCurrentUser(jwt);
    AiItineraryGenerationDto.CreateResponse response = generationService.request(user, itineraryDayId);
    return ApiResponse.of(SuccessCode.ACCEPTED, response).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<AiItineraryGenerationDto.StatusResponse>> getGenerationStatus(
      Jwt jwt, Long generationId) {
    User user = currentUserResolver.getCurrentUser(jwt);
    return ApiResponse.ok(generationService.getStatus(user, generationId)).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<AiItineraryGenerationDto.CreateResponse>> retryGeneration(
      Jwt jwt, Long generationId) {
    User user = currentUserResolver.getCurrentUser(jwt);
    AiItineraryGenerationDto.CreateResponse response = generationService.retry(user, generationId);
    return ApiResponse.of(SuccessCode.ACCEPTED, response)
        .toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<AiItineraryGenerationDto.StatusResponse>> cancelGeneration(
      Jwt jwt, Long generationId) {
    User user = currentUserResolver.getCurrentUser(jwt);
    return ApiResponse.ok(generationService.cancel(user, generationId)).toResponseEntity();
  }
}
