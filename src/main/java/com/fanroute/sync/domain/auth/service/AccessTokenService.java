package com.fanroute.sync.domain.auth.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.fanroute.sync.domain.auth.config.AuthProperties;
import com.fanroute.sync.domain.auth.dto.LoginDto;

import lombok.RequiredArgsConstructor;

/**
 * 로그인한 사용자에게 JWT를 발급합니다.
 */
@Service
@RequiredArgsConstructor
public class AccessTokenService {

  private final JwtEncoder encoder;
  private final AuthProperties properties;
  private final Clock clock;

  /**
   * 사용자 ID를 subject로 사용하고, 권한 판단에 필요한 최소 claim만 포함한 Access Token을 발급합니다.
   */
  public LoginDto.Response issue(AuthPrincipal principal, boolean newUser) {
    IssuedToken issuedToken = issue(principal);
    return LoginDto.Response.of(issuedToken.value(), principal.userId(), newUser,
        issuedToken.expiresIn());
  }

  public IssuedToken issue(AuthPrincipal principal) {
    Instant issuedAt = clock.instant();
    Instant expiresAt = issuedAt.plus(properties.jwt().accessTokenTtl());

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(properties.jwt().issuer())
        .subject(principal.userId().toString())
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .claim("provider", principal.authProvider().name())
        .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    return new IssuedToken(token, properties.jwt().accessTokenTtl().toSeconds());
  }

  public record IssuedToken(String value, long expiresIn) {

  }
}
