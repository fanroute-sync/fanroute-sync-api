package com.fanroute.sync.global.common.response;

import java.util.Collections;
import java.util.List;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

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
        List<ValidationError> errors = bindingResult.getAllErrors().stream()
                .map(ValidationError::of)
                .toList();
        return new ErrorResponse("입력값 검증에 실패하였습니다.", errors);
    }

    // 필드별 상세 에러 정보를 담는 내부 레코드
    public record ValidationError(String field, String reason) {
        public static ValidationError of(ObjectError error) {
            String field = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : null;
            return new ValidationError(field, error.getDefaultMessage());
        }
    }
}
