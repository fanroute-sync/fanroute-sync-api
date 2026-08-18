package com.fanroute.sync.domain.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import tools.jackson.databind.ObjectMapper;

class RefreshTokenCsrfFilterTest {

  @Test
  @DisplayName("Context Path를 제외한 Servlet Path로 보호 경로를 확인한다")
  void matchesServletPathWhenContextPathExists() throws Exception {
    ObjectMapper objectMapper = mock(ObjectMapper.class);
    RefreshTokenCsrfFilter filter = new RefreshTokenCsrfFilter(objectMapper);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/app/api/v1/auth/logout");
    request.setContextPath("/app");
    request.setServletPath("/api/v1/auth/logout");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(403);
    verify(filterChain, never()).doFilter(request, response);
  }
}
