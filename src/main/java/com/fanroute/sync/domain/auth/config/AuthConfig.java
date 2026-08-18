package com.fanroute.sync.domain.auth.config;

import java.net.URI;
import java.time.Clock;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.fanroute.sync.domain.auth.client.GoogleTokenClient;
import com.fanroute.sync.global.config.ExternalApiProperties;
import com.fanroute.sync.global.external.ExternalApiClientFactory;

/**
 * Google OIDC 검증과 서비스 JWT 발급에 필요한 인증 Bean을 구성합니다.
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {

  @Bean
  public GoogleTokenClient googleTokenClient(
      ExternalApiClientFactory factory, ExternalApiProperties externalApiProperties) {

    URI baseUrl = externalApiProperties.getRequiredService("google").getBaseUrl();

    return factory.createClient(
        GoogleTokenClient.class,
        baseUrl);
  }

  /**
   * Google 공개키로 ID Token의 서명, 만료, issuer와 audience를 검증합니다.
   */
  @Bean("googleIdTokenDecoder")
  public JwtDecoder googleIdTokenDecoder(AuthProperties properties) {
    AuthProperties.Google google = properties.google();
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(google.jwkSetUri().toString())
        .build();

    // 내장 검증기(Issuer 등)와 커스텀 Audience 검증기를 체이닝하여 주입
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(google.issuer()),
        createAudienceValidator(google.clientId())));

    return decoder;
  }

  @Bean
  public JwtEncoder jwtEncoder(AuthProperties properties) {
    return NimbusJwtEncoder.withSecretKey(secretKey(properties)).build();
  }

  /**
   * API 요청의 Bearer Token을 서비스 전용 대칭 키와 issuer로 검증합니다.
   */
  @Bean
  public JwtDecoder jwtDecoder(AuthProperties properties) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(properties)).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()));
    return decoder;
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public SecureRandom secureRandom() {
    return new SecureRandom();
  }

  // 정상 서명된 Google 토큰이라도 다른 애플리케이션용 토큰이면 로그인에 사용할 수 없습니다.
  private OAuth2TokenValidator<Jwt> createAudienceValidator(final String clientId) {
    return jwt -> jwt.getAudience().contains(clientId)
        ? OAuth2TokenValidatorResult.success()
        : OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", "Invalid audience", null));
  }

  private SecretKey secretKey(AuthProperties properties) {
    byte[] keyBytes;
    try {
      keyBytes = Base64.getDecoder().decode(properties.jwt().secret());
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("JWT_SECRET must be Base64 encoded", exception);
    }
    // HS256의 보안 강도에 맞춰 최소 256-bit 키를 강제합니다.
    if (keyBytes.length < 32) {
      throw new IllegalStateException("JWT_SECRET must contain at least 256 bits");
    }
    return new SecretKeySpec(keyBytes, "HmacSHA256");
  }
}
