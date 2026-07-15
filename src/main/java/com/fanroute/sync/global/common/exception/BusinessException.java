package com.fanroute.sync.global.common.exception;

import com.fanroute.sync.global.common.response.BaseCode;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final BaseCode errorCode;

    public BusinessException(BaseCode errorCode) {
        super(errorCode.getMessage());

        this.errorCode = errorCode;
    }
}