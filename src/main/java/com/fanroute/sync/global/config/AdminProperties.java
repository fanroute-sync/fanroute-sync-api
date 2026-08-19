package com.fanroute.sync.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "admin")
public record AdminProperties(@NotBlank String apiKey) {

}
