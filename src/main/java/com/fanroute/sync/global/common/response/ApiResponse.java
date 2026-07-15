package com.fanroute.sync.global.common.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String code,
        String message,
        T data) {

    // 자주 사용되는 성공 응답
    public static <T> ApiResponse<T> ok(T data) {
        return of(SuccessCode.OK, data);
    }

    public static <T> ApiResponse<T> ok() {
        return of(SuccessCode.OK, null);
    }

    // 직접 처리할 수 있는 성공 응답
    public static <T> ApiResponse<T> of(BaseCode status, T data) {
        return new ApiResponse<>(true, status.getHttpStatus().value(), null, status.getMessage(), data);
    }

    public static <T> ApiResponse<T> fail(HttpStatus status, String message) {
        return fail(status, null, message);
    }

    public static <T> ApiResponse<T> fail(HttpStatus status, String code, String message) {
        return new ApiResponse<>(false, status.value(), code, message, null);
    }

    // BaseCode(ErrorCode 등) 연동
    public static <T> ApiResponse<T> fail(BaseCode code) {
        return new ApiResponse<>(false, code.getHttpStatus().value(), code.getCode(), code.getMessage(), null);
    }

    // 필드 validation 에러처럼 실패 응답에도 data(상세 정보)를 실어야 할 때
    public static <T> ApiResponse<T> fail(BaseCode code, T data) {
        return new ApiResponse<>(false, code.getHttpStatus().value(), code.getCode(), code.getMessage(), data);
    }

    public ResponseEntity<ApiResponse<T>> toResponseEntity() {
        return ResponseEntity.status(status).body(this);
    }
}