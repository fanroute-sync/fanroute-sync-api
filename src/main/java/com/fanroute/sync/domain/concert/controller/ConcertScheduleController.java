package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.ConcertScheduleDto;
import com.fanroute.sync.domain.concert.service.ConcertScheduleService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ConcertScheduleController implements ConcertScheduleApi {

  private final ConcertScheduleService concertScheduleService;

  @Override
  public ResponseEntity<ApiResponse<List<ConcertScheduleDto.Response>>> getSchedules(
      Long concertId) {
    List<ConcertScheduleDto.Response> schedules = concertScheduleService
        .getSchedulesByConcert(concertId).stream()
        .map(ConcertScheduleDto.Response::from)
        .toList();
    return ApiResponse.ok(schedules).toResponseEntity();
  }
}
