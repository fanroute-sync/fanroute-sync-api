package com.fanroute.sync.global.common.exception;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.response.ErrorCode;
import com.fanroute.sync.global.common.response.ErrorResponse;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("입력값 검증(@Valid) 실패 시, HTTP 400 상태코드와 필드별 상세 에러 목록을 반환한다")
  void handleValidation_ShouldReturnFieldErrorsAndStatus400() {
    // Given: 'request' 객체의 'name' 필드에 "이름은 필수입니다" 에러 가상 바인딩
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    bindingResult.addError(new FieldError("request", "name", "", false, null, null, "이름은 필수입니다."));

    MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
    when(exception.getBindingResult()).thenReturn(bindingResult);

    // When: ExceptionHandler 실행
    ResponseEntity<ApiResponse<ErrorResponse>> entity = handler.handleValidation(exception);
    ApiResponse<ErrorResponse> body = entity.getBody();

    // Then: HTTP Status 400(Bad Request) 및 에러 데이터 상세 검증
    assertAll(
        () -> assertEquals(HttpStatus.BAD_REQUEST, entity.getStatusCode()),
        () -> assertNotNull(body),
        () -> assertFalse(body.success()), // success: false 검증
        () -> assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), body.code()),
        () -> assertEquals(1, body.data().errors().size()),
        () -> assertEquals("name", body.data().errors().get(0).field()),
        () -> assertEquals("이름은 필수입니다.", body.data().errors().get(0).reason())
    );
  }

  @Test
  @DisplayName("비즈니스 예외(BusinessException) 발생 시, 정의된 ErrorCode에 매핑된 HTTP 상태와 공통 포맷을 반환한다")
  void handleBusinessException_ShouldReturnDefinedErrorCodeAndStatus() {
    // Given: 데이터 미존재(DATA_NOT_FOUND) 예외 가상 발생
    BusinessException exception = new BusinessException(ErrorCode.DATA_NOT_FOUND);

    // When: ExceptionHandler 실행
    ResponseEntity<ApiResponse<Void>> entity = handler.handleBusinessException(exception);
    ApiResponse<Void> body = entity.getBody();

    // Then: HTTP Status 404(Not Found) 및 에넘 정의 스펙 일치 확인
    assertAll(
        () -> assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode()),
        () -> assertNotNull(body),
        () -> assertFalse(body.success()),
        () -> assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), body.code()),
        () -> assertEquals(ErrorCode.DATA_NOT_FOUND.getMessage(), body.message())
    );
  }

  @Test
  @DisplayName("예측하지 못한 시스템 에러(Exception) 발생 시, 내부 정보를 마스킹하고 HTTP 500 표준 응답을 반환한다")
  void handleUnexpected_ShouldReturnStatus500AndGenericMessage() {
    // Given: 일반 RuntimeException 가상 발생
    RuntimeException exception = new RuntimeException("DB Connection Timeout!");

    // When: ExceptionHandler 실행
    ResponseEntity<ApiResponse<Void>> entity = handler.handleUnexpected(exception);
    ApiResponse<Void> body = entity.getBody();

    // Then: HTTP Status 500(Internal Server Error) 검증
    assertAll(
        () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode()),
        () -> assertNotNull(body),
        () -> assertFalse(body.success()),
        () -> assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), body.code()),
        () -> assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), body.message())
    );
  }
}