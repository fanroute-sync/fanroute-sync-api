package com.fanroute.sync.domain.schedule.exception;

import org.springframework.http.HttpStatus;

import com.fanroute.sync.global.common.response.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode implements BaseCode {
  TRIP_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE_TRIP_PLAN_NOT_FOUND", "여행 계획을 찾을 수 없습니다."),
  INVALID_TRIP_PERIOD(HttpStatus.BAD_REQUEST, "SCHEDULE_INVALID_TRIP_PERIOD", "여행 기간이 올바르지 않습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
