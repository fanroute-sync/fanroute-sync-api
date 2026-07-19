package com.fanroute.sync.domain.auth.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Google OAuth 자격정보와 서비스 JWT 정책을 {@code auth.*} 설정에서 바인딩합니다.
 */
@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
    @Valid @NotNull Google google,
    @Valid @NotNull Jwt jwt) {

  public record Google(
      @NotBlank String clientId,
      @NotBlank String clientSecret,
      @NotNull URI redirectUri,
      @NotBlank String issuer,
      @NotNull URI jwkSetUri) {

    public MultiValueMap<String, String> toRequestForm(final String code) {
      final MultiValueMap<String, String> form = new org.springframework.util.LinkedMultiValueMap<>();
      form.add("code", code);
      form.add("client_id", this.clientId);
      form.add("client_secret", this.clientSecret);
      form.add("redirect_uri", this.redirectUri.toString());
      form.add("grant_type", "authorization_code");
      return form;
    }
  }

  public record Jwt(
      @NotBlank String issuer,
      @NotBlank String secret,
      @NotNull Duration accessTokenTtl) {

  }
}