package com.fanroute.sync.domain.concert.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fanroute.sync.domain.concert.dto.ConcertScheduleDto;
import com.fanroute.sync.global.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** {@link ConcertScheduleController}의 Swagger 문서 계약. */
@RequestMapping("/api/v1/concert-schedules")
@Tag(name = "공연 회차", description = "공연별 회차(날짜·시간) 조회 API")
public interface ConcertScheduleApi {

  @Operation(summary = "공연별 회차 목록 조회", description = "공연 일자·시각 오름차순으로 조회합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "회차 목록 조회 성공", useReturnTypeSchema = true)
  })
  @GetMapping
  ResponseEntity<ApiResponse<List<ConcertScheduleDto.Response>>> getSchedules(
      @Parameter(description = "대상 공연 ID") @RequestParam Long concertId);
}
