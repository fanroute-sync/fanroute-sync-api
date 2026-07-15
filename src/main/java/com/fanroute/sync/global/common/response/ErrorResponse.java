package com.fanroute.sync.global.common.response;

import java.util.Collections;
import java.util.List;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        String reason,
        List<ValidationError> errors) {

    // 비즈니스 예외용
    public static ErrorResponse of(String reason) {
        return new ErrorResponse(reason, Collections.emptyList());
    }

    // Validation 응답
    public static ErrorResponse of(BindingResult bindingResult) {
        List<ValidationError> errors = bindingResult.getFieldErrors().stream()
                .map(ValidationError::of)
                .toList();
        return new ErrorResponse("입력값 검증에 실패하였습니다.", errors);
    }

    // 필드별 상세 에러 정보를 담는 내부 레코드
    public record ValidationError(String field, Object rejectedValue, String reason) {
        public static ValidationError of(FieldError fieldError) {
            return new ValidationError(
                    fieldError.getField(),
                    fieldError.getRejectedValue(),
                    fieldError.getDefaultMessage());
        }
    }
}