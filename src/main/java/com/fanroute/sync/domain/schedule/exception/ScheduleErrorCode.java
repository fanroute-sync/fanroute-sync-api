package com.fanroute.sync.domain.schedule.exception;

import org.springframework.http.HttpStatus;

import com.fanroute.sync.global.common.response.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode implements BaseCode {
  TRIP_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE_TRIP_PLAN_NOT_FOUND", "여행 계획을 찾을 수 없습니다."),
  ITINERARY_DAY_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE_ITINERARY_DAY_NOT_FOUND", "일정 일자를 찾을 수 없습니다."),
  ITINERARY_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE_ITINERARY_ITEM_NOT_FOUND", "일정 항목을 찾을 수 없습니다."),
  AI_ITINERARY_GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND,
      "SCHEDULE_AI_ITINERARY_GENERATION_NOT_FOUND", "AI 일정 생성 작업을 찾을 수 없습니다."),
  INVALID_AI_ITINERARY_GENERATION_STATUS(HttpStatus.BAD_REQUEST,
      "SCHEDULE_INVALID_AI_ITINERARY_GENERATION_STATUS", "AI 일정 생성 작업 상태가 올바르지 않습니다."),
  INVALID_ITINERARY_ITEM(HttpStatus.BAD_REQUEST, "SCHEDULE_INVALID_ITINERARY_ITEM", "일정 항목 입력이 올바르지 않습니다."),
  FIXED_ITINERARY_ITEM(HttpStatus.BAD_REQUEST, "SCHEDULE_FIXED_ITINERARY_ITEM", "공연 일정 항목은 변경하거나 삭제할 수 없습니다."),
  INVALID_TRIP_PERIOD(HttpStatus.BAD_REQUEST, "SCHEDULE_INVALID_TRIP_PERIOD", "여행 기간이 올바르지 않습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
