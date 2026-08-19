package com.fanroute.sync.domain.concert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Validated
@ConfigurationProperties(prefix = "kopis")
public record KopisProperties(
    @NotBlank String serviceKey,
    @NotBlank String regionCode,
    @PositiveOrZero int syncMaxItems) {

}
