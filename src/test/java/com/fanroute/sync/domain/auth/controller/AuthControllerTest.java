package com.fanroute.sync.domain.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fanroute.sync.domain.auth.config.RefreshTokenCsrfFilter;
import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.dto.RefreshTokenDto;
import com.fanroute.sync.domain.auth.service.GoogleLoginService;
import com.fanroute.sync.domain.auth.service.RefreshTokenService;
import com.fanroute.sync.domain.auth.service.TokenRefreshService;
import com.fanroute.sync.global.config.SecurityConfig;

import jakarta.servlet.http.Cookie;

@WebMvcTest(
    controllers = AuthController.class,
    properties = "auth.refresh.cookie.secure=true")
@Import({SecurityConfig.class, RefreshTokenCookie.class})
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private GoogleLoginService googleLoginService;
  @MockitoBean
  private TokenRefreshService tokenRefreshService;
  @MockitoBean
  private RefreshTokenService refreshTokenService;
  @MockitoBean(name = "jwtDecoder")
  private JwtDecoder jwtDecoder;

  @Test
  @DisplayName("Google 로그인은 Access Token을 응답하고 Refresh Token Cookie를 발급한다")
  void logsInWithRefreshTokenCookie() throws Exception {
    when(googleLoginService.login("authorization-code"))
        .thenReturn(new GoogleLoginService.LoginResult(
            new LoginDto.Response("access-token", "Bearer", 3600, 1L, true),
            new RefreshTokenService.IssuedToken("refresh-token", 1209600)));

    mockMvc.perform(post("/api/v1/auth/google")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"authorizationCode\":\"authorization-code\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").value("access-token"))
        .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
        .andExpect(jsonPath("$.data.newUser").value(true))
        .andExpect(header().string("Set-Cookie", containsString("refreshToken=refresh-token")))
        .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
        .andExpect(header().string("Set-Cookie", containsString("Secure")))
        .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
        .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")));
  }

  @Test
  @DisplayName("Google 인증 코드가 비어 있으면 요청을 거부한다")
  void rejectsBlankAuthorizationCode() throws Exception {
    mockMvc.perform(post("/api/v1/auth/google")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"authorizationCode\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_INVALID_PARAMETER"));
  }

  @Test
  @DisplayName("Refresh Token Cookie로 토큰을 재발급하고 Cookie를 교체한다")
  void refreshesTokenWithCookie() throws Exception {
    when(tokenRefreshService.refresh("refresh-token"))
        .thenReturn(new TokenRefreshService.RefreshResult(
            new RefreshTokenDto.Response("new-access-token", "Bearer", 3600),
            new RefreshTokenService.IssuedToken("new-refresh-token", 1209600)));

    mockMvc.perform(post("/api/v1/auth/token/refresh")
            .header(RefreshTokenCsrfFilter.HEADER_NAME, "XMLHttpRequest")
            .cookie(new Cookie(RefreshTokenCookie.NAME, "refresh-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
        .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
        .andExpect(header().string(
            "Set-Cookie", containsString("refreshToken=new-refresh-token")));
  }

  @Test
  @DisplayName("Refresh Token Cookie가 없으면 재발급을 거부한다")
  void rejectsMissingRefreshTokenCookie() throws Exception {
    mockMvc.perform(post("/api/v1/auth/token/refresh")
            .header(RefreshTokenCsrfFilter.HEADER_NAME, "XMLHttpRequest"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_REFRESH_TOKEN_INVALID"));

    verifyNoInteractions(tokenRefreshService);
  }

  @Test
  @DisplayName("로그아웃은 Refresh Token을 폐기하고 Cookie를 삭제한다")
  void logsOutAndClearsCookie() throws Exception {
    mockMvc.perform(post("/api/v1/auth/logout")
            .header(RefreshTokenCsrfFilter.HEADER_NAME, "XMLHttpRequest")
            .cookie(new Cookie(RefreshTokenCookie.NAME, "a".repeat(43))))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
        .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")));

    verify(refreshTokenService).revoke("a".repeat(43));
  }

  @Test
  @DisplayName("Refresh Token Cookie가 없어도 로그아웃은 멱등하게 성공한다")
  void logsOutWithoutCookie() throws Exception {
    mockMvc.perform(post("/api/v1/auth/logout")
            .header(RefreshTokenCsrfFilter.HEADER_NAME, "XMLHttpRequest"))
        .andExpect(status().isOk())
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
  }

  @Test
  @DisplayName("CSRF 방어 헤더가 없으면 Cookie 인증 요청을 거부한다")
  void rejectsRequestWithoutCsrfHeader() throws Exception {
    mockMvc.perform(post("/api/v1/auth/logout"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_CSRF_HEADER_REQUIRED"));
  }

  @Test
  @DisplayName("보호 API는 인증이 필요하다")
  void protectsOtherEndpoints() throws Exception {
    mockMvc.perform(get("/api/v1/protected"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
  }
}
