package com.fanroute.sync.domain.place.exception;

import org.springframework.http.HttpStatus;

import com.fanroute.sync.global.common.response.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements BaseCode {

  PLACE_NOT_FOUND(
      HttpStatus.NOT_FOUND, "PLACE_NOT_FOUND", "장소를 찾을 수 없습니다."),

  TOUR_API_UNAVAILABLE(
      HttpStatus.BAD_GATEWAY, "PLACE_TOUR_API_UNAVAILABLE", "TourAPI 서버를 사용할 수 없습니다."),

  TOUR_API_RESPONSE_INVALID(
      HttpStatus.BAD_GATEWAY, "PLACE_TOUR_API_RESPONSE_INVALID", "TourAPI 응답을 처리할 수 없습니다."),

  SYNC_ALREADY_RUNNING(
      HttpStatus.CONFLICT, "PLACE_SYNC_ALREADY_RUNNING", "이미 동기화가 진행 중입니다."),

  ADMIN_ACCESS_DENIED(
      HttpStatus.FORBIDDEN, "PLACE_ADMIN_ACCESS_DENIED", "관리자만 접근할 수 있습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
