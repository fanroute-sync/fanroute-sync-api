package com.fanroute.sync.domain.user.exception;

import org.springframework.http.HttpStatus;

import com.fanroute.sync.global.common.response.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseCode {
        // 사용자 입력값 에러
        USER_INVALID_NICKNAME(
                        HttpStatus.BAD_REQUEST,
                        "USER_INVALID_NICKNAME",
                        "닉네임 형식이 올바르지 않습니다."),
        USER_INVALID_PROVIDER_USER_ID(
                        HttpStatus.BAD_REQUEST,
                        "USER_INVALID_PROVIDER_USER_ID",
                        "소셜 로그인 사용자 식별자가 올바르지 않습니다."),
        USER_INVALID_AUTH_PROVIDER(
                        HttpStatus.BAD_REQUEST,
                        "USER_INVALID_AUTH_PROVIDER",
                        "소셜 로그인 제공자가 올바르지 않습니다."),

        // 사용자 조회 및 중복 에러
        USER_NOT_FOUND(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다."),
        USER_DUPLICATE_NICKNAME(
                        HttpStatus.CONFLICT,
                        "USER_DUPLICATE_NICKNAME",
                        "이미 사용 중인 닉네임입니다."),
        USER_DUPLICATE_SOCIAL_ACCOUNT(
                        HttpStatus.CONFLICT,
                        "USER_DUPLICATE_SOCIAL_ACCOUNT",
                        "이미 가입된 소셜 계정입니다."),

        // 사용자 상태 에러
        USER_SUSPENDED(
                        HttpStatus.FORBIDDEN,
                        "USER_SUSPENDED",
                        "정지된 사용자입니다."),
        USER_WITHDRAWN(
                        HttpStatus.FORBIDDEN,
                        "USER_WITHDRAWN",
                        "탈퇴한 사용자입니다."),
        USER_ALREADY_WITHDRAWN(
                        HttpStatus.CONFLICT,
                        "USER_ALREADY_WITHDRAWN",
                        "이미 탈퇴 처리된 사용자입니다.");

        private final HttpStatus httpStatus;
        private final String code;
        private final String message;
}
