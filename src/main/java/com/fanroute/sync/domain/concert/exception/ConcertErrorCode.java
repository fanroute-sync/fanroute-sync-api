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
      HttpStatus.CONFLICT, "CONCERT_SYNC_ALREADY_RUNNING", "이미 동기화가 진행 중입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
