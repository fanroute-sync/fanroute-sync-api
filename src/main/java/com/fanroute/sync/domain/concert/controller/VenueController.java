package com.fanroute.sync.domain.concert.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.VenueDto;
import com.fanroute.sync.domain.concert.service.VenueService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class VenueController implements VenueApi {

  private final VenueService venueService;

  @Override
  public ResponseEntity<ApiResponse<Page<VenueDto.Summary>>> getVenues(int page, int size) {
    Page<VenueDto.Summary> venues = venueService.getVenues(toPageable(page, size))
        .map(VenueDto.Summary::from);
    return ApiResponse.ok(venues).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<VenueDto.Detail>> getVenue(Long venueId) {
    return ApiResponse.ok(VenueDto.Detail.from(
        venueService.getVenue(venueId),
        venueService.countUpcomingConcerts(venueId),
        venueService.countPlaceCollections(venueId))).toResponseEntity();
  }

  private Pageable toPageable(int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "name"));
  }
}
