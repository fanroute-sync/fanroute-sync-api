package com.fanroute.sync.global.common.response;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseCode {
    // 공통 에러
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "C001", "잘못된 파라미터가 포함되었습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "C004", "지원하지 않는 미디어 타입입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C005", "요청한 리소스를 찾을 수 없습니다."),

    // 데이터 관련 에러
    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "요청한 데이터를 찾을 수 없습니다.");

    // 추후 도메인별 예외 추가

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
