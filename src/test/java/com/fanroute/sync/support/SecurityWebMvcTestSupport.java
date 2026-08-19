package com.fanroute.sync.support;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.fanroute.sync.domain.auth.config.AdminAuthoritiesConverter;

/** {@code SecurityConfig}를 Import하는 {@code @WebMvcTest}에 필요한 Mock Bean 모음. */
@TestConfiguration
public class SecurityWebMvcTestSupport {

  @Bean
  public JwtDecoder jwtDecoder() {
    return Mockito.mock(JwtDecoder.class);
  }

  @Bean
  public AdminAuthoritiesConverter adminAuthoritiesConverter() {
    return Mockito.mock(AdminAuthoritiesConverter.class);
  }
}
