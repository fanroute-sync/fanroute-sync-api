package com.fanroute.sync.domain.auth.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.service.GoogleLoginService;
import com.fanroute.sync.global.config.SecurityConfig;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private GoogleLoginService googleLoginService;
  @MockitoBean(name = "jwtDecoder")
  private JwtDecoder jwtDecoder;

  @Test
  @DisplayName("인증 없이 Google 로그인 API에 접근해 Access Token을 받는다")
  void logsInWithoutAuthentication() throws Exception {
    when(googleLoginService.login("authorization-code"))
        .thenReturn(new LoginDto.Response(
            "access-token", "refresh-token", "Bearer", 3600, 1209600, 1L, true));

    mockMvc.perform(post("/api/v1/auth/google")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"authorizationCode\":\"authorization-code\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accessToken").value("access-token"))
        .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
        .andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(1209600))
        .andExpect(jsonPath("$.data.newUser").value(true));
  }

  @Test
  @DisplayName("Google 인가 코드가 비어 있으면 실패 응답을 반환한다")
  void rejectsBlankAuthorizationCode() throws Exception {
    mockMvc.perform(post("/api/v1/auth/google")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"authorizationCode\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @DisplayName("보호된 API는 JWT가 없으면 인증 실패 응답을 반환한다")
  void protectsOtherEndpoints() throws Exception {
    mockMvc.perform(get("/api/v1/protected"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
  }
}
