package com.fanroute.sync.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.fanroute.sync.domain.auth.config.AuthConfig;
import com.fanroute.sync.domain.auth.config.AuthProperties;
import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.support.AuthPropertiesFixture;
import com.fanroute.sync.support.UserFixture;

class AccessTokenServiceTest {

  @Test
  @DisplayName("로그인 사용자 식별자를 포함한 JWT Access Token을 발급한다")
  void issuesAccessToken() {
    AuthProperties properties = AuthPropertiesFixture.defaultProperties();
    AuthConfig config = new AuthConfig();
    Instant now = Instant.now();
    AccessTokenService service = new AccessTokenService(
        config.jwtEncoder(properties), properties, Clock.fixed(now, ZoneOffset.UTC));
    AuthPrincipal principal = AuthPrincipal.from(UserFixture.activeUserWithId(1L));

    LoginDto.Response response = service.issue(principal, true);
    JwtDecoder decoder = config.jwtDecoder(properties);
    Jwt jwt = decoder.decode(response.accessToken());

    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresIn()).isEqualTo(3600);
    assertThat(response.userId()).isEqualTo(1L);
    assertThat(response.newUser()).isTrue();
    assertThat(jwt.getSubject()).isEqualTo("1");
    assertThat(Objects.requireNonNull(jwt.getIssuer()).toString()).isEqualTo("https://api.test.fanroute.com");
    assertThat(jwt.getClaimAsString("provider")).isEqualTo("GOOGLE");
    assertThat(jwt.getExpiresAt())
        .isEqualTo(now.plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS));
  }
}