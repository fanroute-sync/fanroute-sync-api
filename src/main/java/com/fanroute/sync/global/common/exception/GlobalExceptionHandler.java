package com.fanroute.sync.global.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
    log.warn(
        "Validation failed for fields: {}",
        e.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField())
            .distinct()
            .toList());
    ErrorResponse errorResponse = ErrorResponse.of(e.getBindingResult());

    return ApiResponse.fail(ErrorCode.INVALID_PARAMETER, errorResponse).toResponseEntity();
  }

  // 2. 요청 형식 예외
  @ExceptionHandler({
      HttpMessageNotReadableException.class,
      MissingServletRequestParameterException.class,
      MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception e) {
    log.warn("Invalid request: {}", e.getClass().getSimpleName());
    return ApiResponse.<Void>fail(ErrorCode.INVALID_PARAMETER).toResponseEntity();
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException e) {
    log.warn("Unsupported HTTP method: {}", e.getMethod());
    return ApiResponse.<Void>fail(ErrorCode.METHOD_NOT_ALLOWED).toResponseEntity();
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
      HttpMediaTypeNotSupportedException e) {
    log.warn("Unsupported media type: {}", e.getContentType());
    return ApiResponse.<Void>fail(ErrorCode.UNSUPPORTED_MEDIA_TYPE).toResponseEntity();
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
      NoResourceFoundException e) {
    log.warn("Resource not found: {}", e.getClass().getSimpleName());
    return ApiResponse.<Void>fail(ErrorCode.RESOURCE_NOT_FOUND).toResponseEntity();
  }

  // 3. 비즈니스 예외
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
    log.warn("BusinessException: {}", e.getMessage());
    return ApiResponse
        .<Void>fail(e.getErrorCode())
        .toResponseEntity();
  }

  // 4. 시스템 예외
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
    log.error("Unexpected System Error: ", e);
    return ApiResponse
        .<Void>fail(ErrorCode.INTERNAL_SERVER_ERROR)
        .toResponseEntity();
  }
}
