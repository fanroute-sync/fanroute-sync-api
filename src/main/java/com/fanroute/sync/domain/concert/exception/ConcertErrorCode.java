package com.fanroute.sync.domain.concert.exception;

import org.springframework.http.HttpStatus;

import com.fanroute.sync.global.common.response.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConcertErrorCode implements BaseCode {

  CONCERT_NOT_FOUND(
      HttpStatus.NOT_FOUND, "CONCERT_NOT_FOUND", "공연을 찾을 수 없습니다."),

  KOPIS_API_UNAVAILABLE(
      HttpStatus.BAD_GATEWAY, "CONCERT_KOPIS_API_UNAVAILABLE", "KOPIS 서버를 사용할 수 없습니다."),

  KOPIS_RESPONSE_INVALID(
      HttpStatus.BAD_GATEWAY, "CONCERT_KOPIS_RESPONSE_INVALID", "KOPIS 응답을 처리할 수 없습니다."),

  INVALID_GENRE(
      HttpStatus.BAD_REQUEST, "CONCERT_INVALID_GENRE", "존재하지 않는 장르입니다."),

  SYNC_ALREADY_RUNNING(
      HttpStatus.CONFLICT, "CONCERT_SYNC_ALREADY_RUNNING", "이미 동기화가 진행 중입니다."),

  SCHEDULE_NOT_FOUND(
      HttpStatus.NOT_FOUND, "CONCERT_SCHEDULE_NOT_FOUND", "공연 회차를 찾을 수 없습니다."),

  DUPLICATE_SCHEDULE_TIME(
      HttpStatus.CONFLICT, "CONCERT_DUPLICATE_SCHEDULE_TIME", "같은 날짜·시각에 이미 등록된 회차가 있습니다."),

  SCHEDULE_DATE_OUT_OF_RANGE(
      HttpStatus.BAD_REQUEST, "CONCERT_SCHEDULE_DATE_OUT_OF_RANGE", "공연 기간을 벗어난 날짜입니다."),

  RECOMMENDED_PLACE_NOT_FOUND(
      HttpStatus.NOT_FOUND, "CONCERT_RECOMMENDED_PLACE_NOT_FOUND", "추천 장소를 찾을 수 없습니다."),

  DUPLICATE_RECOMMENDED_PLACE(
      HttpStatus.CONFLICT, "CONCERT_DUPLICATE_RECOMMENDED_PLACE",
      "같은 공연장에 이미 등록된 장소 또는 순서가 있습니다."),

  VENUE_NOT_FOUND(
      HttpStatus.NOT_FOUND, "CONCERT_VENUE_NOT_FOUND", "공연장을 찾을 수 없습니다."),

  SCHEDULE_VENUE_MISMATCH(
      HttpStatus.BAD_REQUEST, "CONCERT_SCHEDULE_VENUE_MISMATCH",
      "선택한 회차의 공연장이 요청한 공연장과 일치하지 않습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
