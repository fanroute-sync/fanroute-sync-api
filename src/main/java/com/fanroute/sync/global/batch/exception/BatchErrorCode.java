package com.fanroute.sync.global.batch.exception;

import org.springframework.http.HttpStatus;

import com.fanroute.sync.global.common.response.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BatchErrorCode implements BaseCode {

  JOB_NOT_FOUND(
      HttpStatus.NOT_FOUND, "BATCH_JOB_NOT_FOUND", "존재하지 않는 Job입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
