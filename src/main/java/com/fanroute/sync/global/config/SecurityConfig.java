package com.fanroute.sync.global.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fanroute.sync.domain.auth.config.RefreshTokenCsrfFilter;
import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * JWT 기반 API 인증과 인가 정책을 구성합니다. 로그인 경로만 공개하고, 나머지 요청은 서비스에서 발급한 Bearer Token을 요구합니다.
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      ObjectMapper objectMapper,
      @Qualifier("jwtDecoder") JwtDecoder jwtDecoder,
      CorsConfigurationSource corsConfigurationSource)
      throws Exception {
    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(AbstractHttpConfigurer::disable) // JWT를 사용하므로 CSRF를 비활성화
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers("/api/v1/auth/google", "/api/v1/auth/token/refresh",
                "/api/v1/auth/logout", "/error",
//                           // TODO:: 백엔드에서 확인용으로 작성, 로그인 프론트 연결 시 삭제
                "/api/v1/auth/google/callback"
            ).permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.decoder(jwtDecoder))
            // 401
            .authenticationEntryPoint(
                (request, response, exception) -> writeError(response, objectMapper,
                    AuthErrorCode.AUTHENTICATION_REQUIRED))
            // 403
            .accessDeniedHandler(
                (request, response, exception) -> writeError(response, objectMapper,
                    AuthErrorCode.ACCESS_DENIED)))
        .addFilterBefore(
            new RefreshTokenCsrfFilter(objectMapper),
            BearerTokenAuthenticationFilter.class)
        .build();
  }

  /**
   * 브라우저 클라이언트의 preflight 및 인증 API 호출을 허용하는 CORS 정책입니다.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${cors.allowed-origins}") List<String> allowedOrigins) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", RefreshTokenCsrfFilter.HEADER_NAME));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private void writeError(
      HttpServletResponse response, ObjectMapper objectMapper, AuthErrorCode errorCode)
      throws IOException {
    response.setStatus(errorCode.getHttpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8"); // 한글 깨짐 방지
    objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(errorCode));
  }
}
