package com.fanroute.sync.domain.place.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "tour-api")
public record TourApiProperties(
    @NotBlank String serviceKey,
    @NotBlank String legalDongRegionCode) {

}
