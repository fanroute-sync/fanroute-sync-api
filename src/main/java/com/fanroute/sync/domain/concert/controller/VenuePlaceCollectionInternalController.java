package com.fanroute.sync.domain.concert.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.VenuePlaceCollectionDto;
import com.fanroute.sync.domain.concert.service.VenuePlaceCollectionService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class VenuePlaceCollectionInternalController implements VenuePlaceCollectionInternalApi {

  private final VenuePlaceCollectionService service;

  @Override
  public ResponseEntity<ApiResponse<VenuePlaceCollectionDto.Response>> importCollection(
      VenuePlaceCollectionDto.ImportRequest request) {
    return ApiResponse.ok(service.importCollection(request)).toResponseEntity();
  }
}
