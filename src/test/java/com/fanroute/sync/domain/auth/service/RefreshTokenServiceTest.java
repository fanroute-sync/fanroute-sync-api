package com.fanroute.sync.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fanroute.sync.domain.auth.config.AuthProperties;

class RefreshTokenServiceTest {

  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOperations;
  private RefreshTokenService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    AuthProperties properties = new AuthProperties(
        new AuthProperties.Google(
            "client-id", "client-secret", URI.create("http://localhost/callback"),
            "https://accounts.google.com", URI.create("https://google.test/certs")),
        new AuthProperties.Jwt("issuer", "secret", Duration.ofHours(1)),
        new AuthProperties.Refresh(Duration.ofDays(14)));
    service = new RefreshTokenService(redisTemplate, properties, new SecureRandom());
  }

  @Test
  @DisplayName("Refresh Token 원문 대신 SHA-256 해시를 사용자 키와 함께 TTL로 저장한다")
  void storesHashedRefreshTokenWithTtl() {
    RefreshTokenService.IssuedToken issuedToken = service.issue(1L);
    String tokenHash = RefreshTokenService.hash(issuedToken.value());
    Duration ttl = Duration.ofDays(14);

    assertThat(issuedToken.value()).matches("^[A-Za-z0-9_-]{43}$");
    assertThat(issuedToken.expiresIn()).isEqualTo(ttl.toSeconds());
    verify(valueOperations).set("auth:refresh:" + tokenHash, "1", ttl);
    verify(valueOperations).set("auth:refresh:user:1", tokenHash, ttl);
  }

  @Test
  @DisplayName("사용자의 기존 Refresh Token 해시 키를 제거한 뒤 새 토큰을 저장한다")
  void removesPreviousRefreshToken() {
    when(valueOperations.get("auth:refresh:user:1")).thenReturn("previous-hash");

    service.issue(1L);

    verify(redisTemplate).delete("auth:refresh:previous-hash");
  }
}
