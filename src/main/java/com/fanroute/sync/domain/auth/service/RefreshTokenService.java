package com.fanroute.sync.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fanroute.sync.domain.auth.config.AuthProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private static final int TOKEN_BYTES = 32;
  private static final String TOKEN_KEY_PREFIX = "auth:refresh:";
  private static final String USER_KEY_PREFIX = "auth:refresh:user:";

  private final StringRedisTemplate redisTemplate;
  private final AuthProperties properties;
  private final SecureRandom secureRandom;

  public IssuedToken issue(Long userId) {
    byte[] randomBytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(randomBytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
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
