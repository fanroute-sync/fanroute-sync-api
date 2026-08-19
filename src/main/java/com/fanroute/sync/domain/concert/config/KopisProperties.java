package com.fanroute.sync.domain.concert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "kopis")
public record KopisProperties(
    @NotBlank String serviceKey,
    @NotBlank String regionCode) {

}
