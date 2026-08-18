package com.fanroute.sync.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;

import com.fanroute.sync.domain.auth.client.GoogleTokenClient;
import com.fanroute.sync.domain.auth.config.AuthProperties;
import com.fanroute.sync.domain.auth.dto.GoogleOAuthDto;
import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.service.UserService;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.external.ExternalApiErrorType;
import com.fanroute.sync.global.external.ExternalApiException;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class GoogleLoginServiceTest {

  @Mock
  private GoogleTokenClient googleTokenClient;
  @Mock
  private GoogleIdTokenVerifier idTokenVerifier;
  @Mock
  private UserService userService;
  @Mock
  private AccessTokenService accessTokenService;
  @Mock
  private RefreshTokenService refreshTokenService;

  private GoogleLoginService googleLoginService;

  @BeforeEach
  void setUp() {
    AuthProperties.Google google = new AuthProperties.Google(
        "client-id",
        "client-secret",
        URI.create("http://localhost/callback"),
        "https://accounts.google.com",
        URI.create("https://www.googleapis.com/oauth2/v3/certs"));
    AuthProperties.Jwt jwt = new AuthProperties.Jwt(
        "https://api.test.fanroute.com",
        "dGVzdC1vbmx5LWtleS10aGF0LWlzLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=",
        java.time.Duration.ofHours(1));
    AuthProperties properties = new AuthProperties(
        google, jwt, new AuthProperties.Refresh(java.time.Duration.ofDays(14)));
    googleLoginService = new GoogleLoginService(
        googleTokenClient, idTokenVerifier, userService, accessTokenService, refreshTokenService,
        properties);
  }

  @Test
  @DisplayName("Google 인가 코드로 기존 사용자의 Access Token을 발급한다")
  void logsInExistingUser() {
    GoogleOAuthDto.TokenResponse googleTokens = new GoogleOAuthDto.TokenResponse("access",
        "id-token", 3600,
        "Bearer");
    User user = UserFixture.activeUserWithId(1L);
    LoginDto.Response accessToken = LoginDto.Response.of("service-token", 1L, false, 3600);
    when(googleTokenClient.exchangeToken(any())).thenReturn(googleTokens);
    when(idTokenVerifier.verifyAndExtractUserInfo("id-token"))
        .thenReturn(new GoogleIdTokenVerifier.GoogleUserInfo(
            "google-sub", "user@example.com"));
    when(userService.findOrCreateSocialUser(
        AuthProvider.GOOGLE, "google-sub", "user@example.com"))
        .thenReturn(new UserService.SocialLoginResult(user, false));
    when(accessTokenService.issue(AuthPrincipal.from(user), false)).thenReturn(accessToken);
    when(refreshTokenService.issue(1L))
        .thenReturn(new RefreshTokenService.IssuedToken("refresh-token", 1209600));

    GoogleLoginService.LoginResult result = googleLoginService.login("authorization-code");

    assertThat(result.response()).isEqualTo(accessToken);
    assertThat(result.refreshToken().value()).isEqualTo("refresh-token");
    assertThat(result.refreshToken().expiresIn()).isEqualTo(1209600);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(
        MultiValueMap.class);
    verify(googleTokenClient).exchangeToken(formCaptor.capture());
    verify(userService).findOrCreateSocialUser(
        AuthProvider.GOOGLE, "google-sub", "user@example.com");
    assertThat(formCaptor.getValue().toSingleValueMap()).containsAllEntriesOf(Map.of(
        "code", "authorization-code",
        "client_id", "client-id",
        "client_secret", "client-secret",
        "redirect_uri", "http://localhost/callback",
        "grant_type", "authorization_code"));
  }

  @Test
  @DisplayName("잘못된 Google 인가 코드를 인증 실패로 변환한다")
  void rejectsInvalidAuthorizationCode() {
    when(googleTokenClient.exchangeToken(any())).thenThrow(
        ExternalApiException.responseError(
            ExternalApiErrorType.CLIENT_ERROR, HttpStatus.BAD_REQUEST,
            "bad request"));

    assertThatThrownBy(() -> googleLoginService.login("invalid-code"))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.GOOGLE_AUTHORIZATION_CODE_INVALID));
  }

  @Test
  @DisplayName("Google 응답에 ID Token이 없으면 인증에 실패한다")
  void rejectsMissingIdToken() {
    when(googleTokenClient.exchangeToken(any()))
        .thenReturn(new GoogleOAuthDto.TokenResponse("access", "", 3600, "Bearer"));

    assertThatThrownBy(() -> googleLoginService.login("authorization-code"))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.GOOGLE_ID_TOKEN_INVALID));
  }

  @Test
  @DisplayName("Google 서버 오류를 외부 인증 서버 오류로 변환한다")
  void handlesGoogleServerError() {
    when(googleTokenClient.exchangeToken(any())).thenThrow(
        ExternalApiException.responseError(
            ExternalApiErrorType.SERVER_ERROR, HttpStatus.BAD_GATEWAY,
            "server error"));

    assertThatThrownBy(() -> googleLoginService.login("authorization-code"))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.GOOGLE_API_UNAVAILABLE));
  }
}