package com.fanroute.sync.domain.auth.exception;

import org.springframework.http.HttpStatus;

import com.fanroute.sync.global.common.response.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Google 인증, 서비스 JWT 인증 및 접근 거부에 사용하는 공통 오류 코드입니다.
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseCode {
  GOOGLE_AUTHORIZATION_CODE_INVALID(
      HttpStatus.UNAUTHORIZED, "AUTH_GOOGLE_CODE_INVALID", "Google 인가 코드가 유효하지 않습니다."),
  GOOGLE_ID_TOKEN_INVALID(
      HttpStatus.UNAUTHORIZED, "AUTH_GOOGLE_ID_TOKEN_INVALID", "Google ID Token이 유효하지 않습니다."),
  GOOGLE_API_UNAVAILABLE(
      HttpStatus.BAD_GATEWAY, "AUTH_GOOGLE_API_UNAVAILABLE", "Google 인증 서버를 사용할 수 없습니다."),
  AUTHENTICATION_REQUIRED(
      HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "인증이 필요합니다."),
  ACCESS_TOKEN_INVALID(
      HttpStatus.UNAUTHORIZED, "AUTH_ACCESS_TOKEN_INVALID", "Access Token이 유효하지 않습니다."),
  REFRESH_TOKEN_INVALID(
      HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_INVALID", "Refresh Token이 유효하지 않습니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_ACCESS_DENIED", "접근 권한이 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
