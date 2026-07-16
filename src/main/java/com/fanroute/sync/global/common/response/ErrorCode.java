package com.fanroute.sync.global.common.response;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseCode {
    // 공통 에러
    INVALID_PARAMETER(
            HttpStatus.BAD_REQUEST, "COMMON_INVALID_PARAMETER", "잘못된 파라미터가 포함되었습니다."),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    METHOD_NOT_ALLOWED(
            HttpStatus.METHOD_NOT_ALLOWED, "COMMON_METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON_UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 미디어 타입입니다."),
    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND, "COMMON_RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),

    // 데이터 관련 에러
    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "DATA_NOT_FOUND", "요청한 데이터를 찾을 수 없습니다."),

    // 사용자 에러
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    USER_DUPLICATE_NICKNAME(
            HttpStatus.CONFLICT, "USER_DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다."),
    USER_DUPLICATE_SOCIAL_ACCOUNT(
            HttpStatus.CONFLICT, "USER_DUPLICATE_SOCIAL_ACCOUNT", "이미 가입된 소셜 계정입니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "USER_SUSPENDED", "정지된 사용자입니다."),
    USER_WITHDRAWN(HttpStatus.FORBIDDEN, "USER_WITHDRAWN", "탈퇴한 사용자입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
