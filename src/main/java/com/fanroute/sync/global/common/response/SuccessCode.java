package com.fanroute.sync.global.common.response;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements BaseCode {
    OK(HttpStatus.OK, HttpStatus.OK.getReasonPhrase(), "요청이 성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, HttpStatus.CREATED.getReasonPhrase(), "리소스가 성공적으로 생성되었습니다."),
    ACCEPTED(HttpStatus.ACCEPTED, HttpStatus.ACCEPTED.getReasonPhrase(), "요청이 접수되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
