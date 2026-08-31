package com.fanroute.sync.domain.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.service.GoogleLoginService;
import com.fanroute.sync.domain.auth.service.RefreshTokenService;
import com.fanroute.sync.global.config.SecurityConfig;
import com.fanroute.sync.support.SecurityWebMvcTestSupport;

@WebMvcTest(controllers = AuthDebugController.class)
@Import({SecurityConfig.class, RefreshTokenCookie.class, SecurityWebMvcTestSupport.class})
@ActiveProfiles("local")
class AuthDebugControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private GoogleLoginService googleLoginService;

  @Test
  @DisplayName("local 프로파일에서는 콜백이 로그인 처리 후 Refresh Token Cookie를 발급한다")
  void logsInAndIssuesCookie() throws Exception {
    when(googleLoginService.login("authorization-code"))
        .thenReturn(new GoogleLoginService.LoginResult(
            new LoginDto.Response("access-token", "Bearer", 3600, 1L, true),
            new RefreshTokenService.IssuedToken("refresh-token", 1209600)));

    mockMvc.perform(get("/api/v1/auth/google/callback")
            .param("code", "authorization-code"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").value("access-token"))
        .andExpect(header().string("Set-Cookie", containsString("refreshToken=refresh-token")));
  }
}
