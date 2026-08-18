package com.fanroute.sync.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.fanroute.sync.domain.auth.config.AuthProperties;
import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final int TOKEN_BYTES = 32;
  private static final String TOKEN_KEY_PREFIX = "auth:refresh:";
  private static final String USER_KEY_PREFIX = "auth:refresh:user:";
  private static final String ROTATE_SCRIPT = """
      local userId = redis.call('GET', KEYS[1])
      local currentHash = redis.call('GET', KEYS[2])
      if not userId or userId ~= ARGV[2] or not currentHash or currentHash ~= ARGV[1] then
        return 0
      end
      redis.call('DEL', KEYS[1])
      redis.call('SET', KEYS[3], ARGV[2], 'PX', ARGV[4])
      redis.call('SET', KEYS[2], ARGV[3], 'PX', ARGV[4])
      return 1
      """;
  private static final DefaultRedisScript<Long> ROTATE_REDIS_SCRIPT =
      new DefaultRedisScript<>(ROTATE_SCRIPT, Long.class);
  private static final String REVOKE_SCRIPT = """
      local userId = redis.call('GET', KEYS[1])
      if not userId then
        return 0
      end
      local userKey = ARGV[1] .. userId
      if redis.call('GET', userKey) == ARGV[2] then
        redis.call('DEL', userKey)
      end
      redis.call('DEL', KEYS[1])
      return 1
      """;
  private static final DefaultRedisScript<Long> REVOKE_REDIS_SCRIPT =
      new DefaultRedisScript<>(REVOKE_SCRIPT, Long.class);
  private static final String REVOKE_ALL_SCRIPT = """
      local tokenHash = redis.call('GET', KEYS[1])
      if not tokenHash then
        return 0
      end
      redis.call('DEL', ARGV[1] .. tokenHash)
      redis.call('DEL', KEYS[1])
      return 1
      """;
  private static final DefaultRedisScript<Long> REVOKE_ALL_REDIS_SCRIPT =
      new DefaultRedisScript<>(REVOKE_ALL_SCRIPT, Long.class);

  private final StringRedisTemplate redisTemplate;
  private final AuthProperties properties;
  private final SecureRandom secureRandom;

  public IssuedToken issue(Long userId) {
    String token = generateToken();
    String tokenHash = hash(token);
    Duration ttl = properties.refresh().tokenTtl();
    String userKey = USER_KEY_PREFIX + userId;

    String previousHash = redisTemplate.opsForValue().get(userKey);
    if (previousHash != null) {
      redisTemplate.delete(TOKEN_KEY_PREFIX + previousHash);
    }

    redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + tokenHash, userId.toString(), ttl);
    redisTemplate.opsForValue().set(userKey, tokenHash, ttl);

    return new IssuedToken(token, ttl.toSeconds());
  }

  public Long findUserId(String token) {
    validateFormat(token);
    String userId = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + hash(token));
    try {
      if (userId != null) {
        return Long.valueOf(userId);
      }
    } catch (NumberFormatException ignored) {
      // 손상되었거나 조작된 Redis 값은 유효하지 않은 토큰과 동일하게 처리합니다.
    }
    throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
  }

  public IssuedToken rotate(String token, Long userId) {
    validateFormat(token);
    String oldHash = hash(token);
    String newToken = generateToken();
    String newHash = hash(newToken);
    Duration ttl = properties.refresh().tokenTtl();
    Long rotated = redisTemplate.execute(
        ROTATE_REDIS_SCRIPT,
        java.util.List.of(
            TOKEN_KEY_PREFIX + oldHash,
            USER_KEY_PREFIX + userId,
            TOKEN_KEY_PREFIX + newHash),
        oldHash, userId.toString(), newHash, Long.toString(ttl.toMillis()));
    if (!Long.valueOf(1L).equals(rotated)) {
      throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
    }
    return new IssuedToken(newToken, ttl.toSeconds());
  }

  public void revoke(String token) {
    validateFormat(token);
    String tokenHash = hash(token);
    redisTemplate.execute(
        REVOKE_REDIS_SCRIPT,
        java.util.List.of(TOKEN_KEY_PREFIX + tokenHash),
        USER_KEY_PREFIX, tokenHash);
  }

  public void revokeAll(Long userId) {
    redisTemplate.execute(
        REVOKE_ALL_REDIS_SCRIPT,
        java.util.List.of(USER_KEY_PREFIX + userId),
        TOKEN_KEY_PREFIX);
  }

  private String generateToken() {
    byte[] randomBytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  private void validateFormat(String token) {
    if (token == null || !token.matches("^[A-Za-z0-9_-]{43}$")) {
      throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
    }
  }

  static String hash(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  public record IssuedToken(String value, long expiresIn) {

  }
}
