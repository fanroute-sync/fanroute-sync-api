package com.fanroute.sync.global.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fanroute.sync.domain.auth.config.AdminAuthoritiesConverter;
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
      JwtAuthenticationConverter jwtAuthenticationConverter,
      CorsConfigurationSource corsConfigurationSource)
      throws Exception {
    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(AbstractHttpConfigurer::disable) // JWT를 사용하므로 CSRF를 비활성화
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            // 브라우저 업그레이드 이후 STOMP CONNECT에서 JWT를 검증한다.
            .requestMatchers(HttpMethod.GET, "/ws/chat").permitAll()
            .requestMatchers("/api/v1/auth/google", "/api/v1/auth/token/refresh",
                "/api/v1/auth/logout", "/error",
                // local 프로파일에서만 컨트롤러가 등록됨(AuthDebugController) — 운영에는 이 경로 자체가 없음
                "/api/v1/auth/google/callback"
            ).permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/concerts", "/api/v1/concerts/*")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/places", "/api/v1/places/*")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/concert-schedules")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/venues/*/place-collections")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/venues/*/itinerary-templates")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/venues", "/api/v1/venues/*")
            .permitAll()
            .requestMatchers("/api/v1/internal/**").hasRole("ADMIN")
            // 관리자 경로의 권한 검사를 한 곳에서 강제합니다.
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter))
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

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter(
      AdminAuthoritiesConverter adminAuthoritiesConverter) {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(adminAuthoritiesConverter::resolveAuthorities);
    return converter;
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
