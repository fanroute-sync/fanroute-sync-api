package com.fanroute.sync.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.auth.config.AuthConfig;
import com.fanroute.sync.domain.auth.config.AuthProperties;
import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;

class AccessTokenServiceTest {

  @Test
  @DisplayName("로그인 사용자 식별자를 포함한 JWT Access Token을 발급한다")
  void issuesAccessToken() {
    AuthProperties properties = properties();
    AuthConfig config = new AuthConfig();
    Instant now = Instant.now();
    AccessTokenService service = new AccessTokenService(
        config.jwtEncoder(properties), properties, Clock.fixed(now, ZoneOffset.UTC));
    User user = User.create("route", AuthProvider.GOOGLE, "google-sub");
    ReflectionTestUtils.setField(user, "id", 1L);

    LoginDto.Response response = service.issue(user, true);
    JwtDecoder decoder = config.jwtDecoder(properties);
    Jwt jwt = decoder.decode(response.accessToken());

    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresIn()).isEqualTo(3600);
    assertThat(response.userId()).isEqualTo(1L);
    assertThat(response.newUser()).isTrue();
    assertThat(jwt.getSubject()).isEqualTo("1");
    assertThat(jwt.getIssuer().toString()).isEqualTo("https://api.test.fanroute.com");
    assertThat(jwt.getClaimAsString("provider")).isEqualTo("GOOGLE");
    assertThat(jwt.getExpiresAt())
        .isEqualTo(now.plusSeconds(3600).truncatedTo(ChronoUnit.SECONDS));
  }

  private AuthProperties properties() {
    AuthProperties.Google google = new AuthProperties.Google(
        "test-client",
        "test-secret",
        java.net.URI.create("http://localhost/callback"),
        "https://accounts.google.com",
        java.net.URI.create("https://www.googleapis.com/oauth2/v3/certs"));
    AuthProperties.Jwt jwt = new AuthProperties.Jwt(
        "https://api.test.fanroute.com",
        "dGVzdC1vbmx5LWtleS10aGF0LWlzLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=",
        Duration.ofHours(1));
    return new AuthProperties(google, jwt, new AuthProperties.Refresh(Duration.ofDays(14)));
  }
}
