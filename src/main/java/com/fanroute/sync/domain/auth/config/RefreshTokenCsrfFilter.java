package com.fanroute.sync.domain.auth.config;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class RefreshTokenCsrfFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-Requested-With";
  private static final Set<String> PROTECTED_PATHS = Set.of(
      "/api/v1/auth/token/refresh",
      "/api/v1/auth/logout");

  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (requiresHeader(request) && !StringUtils.hasText(request.getHeader(HEADER_NAME))) {
      response.setStatus(AuthErrorCode.CSRF_HEADER_REQUIRED.getHttpStatus().value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      objectMapper.writeValue(
          response.getOutputStream(), ApiResponse.fail(AuthErrorCode.CSRF_HEADER_REQUIRED));
      return;
    }
    filterChain.doFilter(request, response);
  }

  private boolean requiresHeader(HttpServletRequest request) {
    String requestPath =
        request.getRequestURI().substring(request.getContextPath().length());

    return HttpMethod.POST.matches(request.getMethod())
        && PROTECTED_PATHS.contains(requestPath);
  }
}
