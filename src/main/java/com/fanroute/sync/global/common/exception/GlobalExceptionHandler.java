package com.fanroute.sync.global.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.response.ErrorCode;
import com.fanroute.sync.global.common.response.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  // 1. @RequestBody JSON 유효성 실패 (@Valid)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<ErrorResponse>> handleValidation(
      MethodArgumentNotValidException e) {
    log.warn("Validation failed: {}", e.getMessage());
    ErrorResponse errorResponse = ErrorResponse.of(e.getBindingResult());

    return ApiResponse.fail(ErrorCode.INVALID_PARAMETER, errorResponse).toResponseEntity();
  }

  // 2. 비즈니스 예외
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
    log.warn("BusinessException: {}", e.getMessage());
    return ApiResponse
        .<Void>fail(e.getErrorCode())
        .toResponseEntity();
  }

  // 3. 시스템 예외
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
    log.error("Unexpected System Error: ", e);
    return ApiResponse
        .<Void>fail(ErrorCode.INTERNAL_SERVER_ERROR)
        .toResponseEntity();
  }
}