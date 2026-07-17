package com.fanroute.sync.global.util;

import org.springframework.util.StringUtils;

import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.BaseCode;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TextValidator {

    /**
     * 필수 문자열이 유효한지 검증하고 앞뒤 공백을 제거합니다.
     *
     * <p>
     * {@code null}, 빈 문자열, 공백으로만 구성된 문자열은
     * 유효하지 않은 값으로 처리합니다.
     * </p>
     *
     * @param value     검증하고 정규화할 문자열
     * @param errorCode 검증 실패 시 사용할 에러 코드
     * @return 앞뒤 공백이 제거된 문자열
     * @throws BusinessException 문자열이 null이거나 유효한 문자를 포함하지 않는 경우
     */
    public static String normalizeRequired(
            String value,
            BaseCode errorCode) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(errorCode);
        }

        return value.trim();
    }

    /**
     * 필수 문자열이 유효하고 최대 길이를 초과하지 않는지 검증한 후
     * 앞뒤 공백을 제거합니다.
     *
     * <p>
     * 문자열의 길이는 앞뒤 공백을 제거한 값을 기준으로 검사합니다.
     * </p>
     *
     * @param value     검증하고 정규화할 문자열
     * @param maxLength 허용할 최대 문자열 길이
     * @param errorCode 검증 실패 시 사용할 에러 코드
     * @return 앞뒤 공백이 제거되고 최대 길이 조건을 만족하는 문자열
     * @throws BusinessException 문자열이 비어 있거나 최대 길이를 초과한 경우
     */
    public static String normalizeRequired(
            String value,
            int maxLength,
            BaseCode errorCode) {
        String normalized = normalizeRequired(value, errorCode);

        if (normalized.length() > maxLength) {
            throw new BusinessException(errorCode);
        }

        return normalized;
    }

    public static <T> T requireNonNull(T value, BaseCode errorCode) {
        if (value == null) {
            throw new BusinessException(errorCode);
        }
        return value;
    }
}
