package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.VenuePlaceCollectionDto;
import com.fanroute.sync.domain.concert.service.VenuePlaceCollectionService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class VenuePlaceCollectionController implements VenuePlaceCollectionApi {

  private final VenuePlaceCollectionService service;

  @Override
  public ResponseEntity<ApiResponse<List<VenuePlaceCollectionDto.Response>>> getCollections(
      Long venueId) {
    return ApiResponse.ok(service.getCollections(venueId)).toResponseEntity();
  }
}
