package com.fanroute.sync.domain.place.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.place.dto.PlaceDto;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.service.PlaceService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PlaceController implements PlaceApi {

  private final PlaceService placeService;

  @Override
  public ResponseEntity<ApiResponse<Page<PlaceDto.Response>>> getPlaces(
      PlaceCategory category, int page, int size) {
    Pageable pageable = toPageable(page, size);
    Page<PlaceDto.Response> places =
        placeService.getPlaces(category, pageable).map(PlaceDto.Response::from);
    return ApiResponse.ok(places).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<PlaceDto.Response>> getPlace(Long placeId) {
    Place place = placeService.getPlace(placeId);
    return ApiResponse.ok(PlaceDto.Response.from(place)).toResponseEntity();
  }

  private Pageable toPageable(int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "id"));
  }
}
