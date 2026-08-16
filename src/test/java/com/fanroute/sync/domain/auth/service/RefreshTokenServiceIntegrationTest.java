package com.fanroute.sync.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fanroute.sync.domain.auth.config.AuthProperties;
import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;

@Testcontainers(disabledWithoutDocker = true)
class RefreshTokenServiceIntegrationTest {

  private static final int REDIS_PORT = 6379;
  private static final Duration TOKEN_TTL = Duration.ofDays(14);

  @Container
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(REDIS_PORT);

  private static LettuceConnectionFactory connectionFactory;
  private static StringRedisTemplate redisTemplate;

  private RefreshTokenService service;

  @BeforeAll
  static void setUpRedis() {
    connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT));
    connectionFactory.afterPropertiesSet();
    redisTemplate = new StringRedisTemplate(connectionFactory);
    redisTemplate.afterPropertiesSet();
  }

  @AfterAll
  static void closeRedisConnection() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
  }

  @BeforeEach
  void setUp() {
    redisTemplate.execute((RedisCallback<Void>) connection -> {
      connection.serverCommands().flushAll();
      return null;
    });
    service = new RefreshTokenService(redisTemplate, properties(), new SecureRandom());
  }

  @Test
  @DisplayName("동일한 Refresh Token의 동시 Rotation은 하나만 성공한다")
  void allowsOnlyOneConcurrentRotation() throws Exception {
    RefreshTokenService.IssuedToken original = service.issue(1L);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    Callable<RefreshTokenService.IssuedToken> rotation = () -> {
      ready.countDown();
      start.await();
      try {
        return service.rotate(original.value(), 1L);
      } catch (BusinessException exception) {
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_INVALID);
        return null;
      }
    };

    List<RefreshTokenService.IssuedToken> results;
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      List<Future<RefreshTokenService.IssuedToken>> futures =
          List.of(executor.submit(rotation), executor.submit(rotation));
      ready.await();
      start.countDown();
      results = futures.stream().map(this::getResult).toList();
    }

    List<RefreshTokenService.IssuedToken> successful =
        results.stream().filter(result -> result != null).toList();
    assertThat(successful).hasSize(1);
    assertThatThrownBy(() -> service.findUserId(original.value()))
        .isInstanceOf(BusinessException.class);
    assertThat(redisTemplate.hasKey(
        "auth:refresh:" + RefreshTokenService.hash(original.value()))).isFalse();

    RefreshTokenService.IssuedToken rotated = successful.getFirst();
    String newHash = RefreshTokenService.hash(rotated.value());
    assertThat(service.findUserId(rotated.value())).isEqualTo(1L);
    assertThat(redisTemplate.opsForValue().get("auth:refresh:user:1")).isEqualTo(newHash);
    assertThat(redisTemplate.getExpire("auth:refresh:" + newHash))
        .isBetween(TOKEN_TTL.toSeconds() - 30, TOKEN_TTL.toSeconds());
    assertThat(redisTemplate.getExpire("auth:refresh:user:1"))
        .isBetween(TOKEN_TTL.toSeconds() - 30, TOKEN_TTL.toSeconds());
  }

  @Test
  @DisplayName("사용자 인덱스가 기존 토큰 해시와 일치하지 않으면 Rotation을 거부한다")
  void rejectsRotationWhenUserIndexDoesNotMatch() {
    RefreshTokenService.IssuedToken original = service.issue(1L);
    redisTemplate.opsForValue().set("auth:refresh:user:1", "different-hash", TOKEN_TTL);

    assertThatThrownBy(() -> service.rotate(original.value(), 1L))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_INVALID));
  }

  private RefreshTokenService.IssuedToken getResult(
      Future<RefreshTokenService.IssuedToken> future) {
    try {
      return future.get();
    } catch (Exception exception) {
      throw new AssertionError("동시 Rotation 실행에 실패했습니다.", exception);
    }
  }

  private static AuthProperties properties() {
    return new AuthProperties(
        new AuthProperties.Google(
            "client-id", "client-secret", URI.create("http://localhost/callback"),
            "https://accounts.google.com", URI.create("https://google.test/certs")),
        new AuthProperties.Jwt("issuer", "secret", Duration.ofHours(1)),
        new AuthProperties.Refresh(TOKEN_TTL));
  }
}
