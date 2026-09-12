package com.fanroute.sync.domain.concert.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.concert.dto.ConcertScheduleDto;
import com.fanroute.sync.domain.concert.service.ConcertScheduleService;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ConcertScheduleAdminController implements ConcertScheduleAdminApi {

  private final ConcertScheduleService concertScheduleService;

  @Override
  public ResponseEntity<ApiResponse<ConcertScheduleDto.Response>> createSchedule(
      ConcertScheduleDto.CreateRequest request) {
    return ApiResponse.ok(ConcertScheduleDto.Response.from(
        concertScheduleService.createSchedule(request))).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<ConcertScheduleDto.Response>> updateSchedule(
      Long scheduleId, ConcertScheduleDto.UpdateRequest request) {
    return ApiResponse.ok(ConcertScheduleDto.Response.from(
        concertScheduleService.updateSchedule(scheduleId, request))).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> deleteSchedule(Long scheduleId) {
    concertScheduleService.deleteSchedule(scheduleId);
    return ApiResponse.<Void>ok().toResponseEntity();
  }
}
