package com.fanroute.sync.domain.concert.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.concert.dto.ConcertScheduleDto;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** {@link ConcertScheduleAdminController}의 Swagger 문서 계약. */
@RequestMapping("/api/v1/admin/concert-schedules")
@Tag(name = "공연 회차 관리", description = "관리자 전용 공연 회차 생성·수정·삭제 API")
@SecurityRequirement(name = "bearerAuth")
public interface ConcertScheduleAdminApi {

  @Operation(summary = "공연 회차 생성", description = "ADMIN 권한을 가진 사용자만 공연 회차(날짜·시간)를 등록합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "회차 생성 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = ConcertErrorCode.class,
      names = {"CONCERT_NOT_FOUND", "DUPLICATE_SCHEDULE_TIME", "SCHEDULE_DATE_OUT_OF_RANGE"})
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PostMapping
  ResponseEntity<ApiResponse<ConcertScheduleDto.Response>> createSchedule(
      @Valid @RequestBody ConcertScheduleDto.CreateRequest request);

  @Operation(summary = "공연 회차 수정")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "회차 수정 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = ConcertErrorCode.class,
      names = {"SCHEDULE_NOT_FOUND", "DUPLICATE_SCHEDULE_TIME", "SCHEDULE_DATE_OUT_OF_RANGE"})
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @PutMapping("/{scheduleId}")
  ResponseEntity<ApiResponse<ConcertScheduleDto.Response>> updateSchedule(
      @PathVariable Long scheduleId, @Valid @RequestBody ConcertScheduleDto.UpdateRequest request);

  @Operation(summary = "공연 회차 삭제")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "회차 삭제 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(type = ConcertErrorCode.class, names = "SCHEDULE_NOT_FOUND")
  @ApiErrorCodeExamples(type = AuthErrorCode.class, names = "ACCESS_DENIED")
  @DeleteMapping("/{scheduleId}")
  ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable Long scheduleId);
}
