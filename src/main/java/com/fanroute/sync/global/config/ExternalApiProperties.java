package com.fanroute.sync.global.config;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 외부 API용 설정 값을 바인딩합니다.
 * <p>
 * {@code application.yml}의 {@code external-api} 하위 설정을 변환하여 HTTP Client 설정과
 * 서비스별 Base URL에 사용합니다.
 * </p>
 */
@Getter
@Validated
@ConfigurationProperties(prefix = "external-api")
public class ExternalApiProperties {

    @Valid
    private final Http http = new Http();
    private final Map<String, @Valid Service> services = new HashMap<>(); // Key: 외부 서비스 이름, Value: 내용

    /**
     * 등록된 외부 서비스 설정을 반환합니다.
     *
     * @param serviceName 조회할 서비스 이름
     * @return 해당 외부 서비스 설정
     * @throws IllegalArgumentException 등록되지 않은 서비스 이름인 경우
     */
    public Service getRequiredService(String serviceName) {
        Service service = services.get(serviceName);
        if (service == null) {
            throw new IllegalArgumentException("Unknown external API service: " + serviceName);
        }
        return service;
    }

    /**
     * 공통 클라이언트 Time out 설정
     */
    @Getter
    @Setter
    public static class Http {

        @NotNull
        private Duration connectTimeout = Duration.ofSeconds(3);
        @NotNull
        private Duration readTimeout = Duration.ofSeconds(5);
    }

    /**
     * 각각 서비스 개별 설정
     */
    @Getter
    @Setter
    public static class Service {

        @NotNull
        private URI baseUrl;
    }
}
