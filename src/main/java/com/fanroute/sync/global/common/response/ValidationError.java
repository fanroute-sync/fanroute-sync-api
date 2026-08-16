package com.fanroute.sync.global.common.response;

import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

public record ValidationError(String field, String message) {

  public static ValidationError from(ObjectError error) {
    String field = error instanceof FieldError fieldError
        ? fieldError.getField()
        : null;
    return new ValidationError(field, error.getDefaultMessage());
  }
}
