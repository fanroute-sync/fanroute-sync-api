package com.fanroute.sync.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.fanroute.sync.domain.auth.service.GoogleLoginService;

/** local 프로파일이 아니면(운영 포함) 이 디버그 콜백 자체가 컨텍스트에 등록되지 않는지 검증합니다. */
class AuthDebugControllerProfileTest {

  @Test
  @DisplayName("local 프로파일이 아니면 AuthDebugController가 등록되지 않는다")
  void doesNotRegisterWithoutLocalProfile() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(AuthDebugController.class, RefreshTokenCookie.class);
      context.registerBean(GoogleLoginService.class, () -> Mockito.mock(GoogleLoginService.class));
      context.refresh();

      assertThat(context.getBeanNamesForType(AuthDebugController.class)).isEmpty();
    }
  }

  @Test
  @DisplayName("local 프로파일이면 AuthDebugController가 등록된다")
  void registersWithLocalProfile() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.getEnvironment().setActiveProfiles("local");
      context.register(AuthDebugController.class, RefreshTokenCookie.class);
      context.registerBean(GoogleLoginService.class, () -> Mockito.mock(GoogleLoginService.class));
      context.refresh();

      assertThat(context.getBeanNamesForType(AuthDebugController.class)).hasSize(1);
    }
  }
}
