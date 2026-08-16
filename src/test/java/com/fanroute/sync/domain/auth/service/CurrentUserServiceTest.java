package com.fanroute.sync.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.domain.user.service.UserService;
import com.fanroute.sync.global.common.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

  @Mock
  private UserService userService;

  private CurrentUserService currentUserService;

  @BeforeEach
  void setUp() {
    currentUserService = new CurrentUserService(userService);
  }

  @Test
  @DisplayName("JWT sub의 사용자 ID로 현재 사용자를 조회한다")
  void getsCurrentUserFromJwtSubject() {
    User user = User.create("route", AuthProvider.GOOGLE, "google-1");
    when(userService.getAccessibleUser(1L)).thenReturn(user);

    User currentUser = currentUserService.getCurrentUser(jwt("1"));

    assertSame(user, currentUser);
    verify(userService).getAccessibleUser(1L);
  }

  @Test
  @DisplayName("JWT sub가 숫자 형식이 아니면 유효하지 않은 Access Token으로 처리한다")
  void rejectsInvalidJwtSubject() {
    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> currentUserService.getCurrentUser(jwt("invalid")));

    assertEquals(AuthErrorCode.ACCESS_TOKEN_INVALID, exception.getErrorCode());
  }

  @Test
  @DisplayName("JWT sub에 해당하는 사용자가 없으면 접근할 수 없다")
  void rejectsMissingUser() {
    assertUserAccessFailure(UserErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("정지된 현재 사용자는 접근할 수 없다")
  void rejectsSuspendedUser() {
    assertUserAccessFailure(UserErrorCode.USER_SUSPENDED);
  }

  @Test
  @DisplayName("탈퇴한 현재 사용자는 접근할 수 없다")
  void rejectsWithdrawnUser() {
    assertUserAccessFailure(UserErrorCode.USER_WITHDRAWN);
  }

  private void assertUserAccessFailure(UserErrorCode errorCode) {
    when(userService.getAccessibleUser(1L)).thenThrow(new BusinessException(errorCode));

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> currentUserService.getCurrentUser(jwt("1")));

    assertEquals(errorCode, exception.getErrorCode());
  }

  private Jwt jwt(String subject) {
    Instant issuedAt = Instant.now();
    return new Jwt(
        "token",
        issuedAt,
        issuedAt.plusSeconds(60),
        Map.of("alg", "HS256"),
        Map.of("sub", subject));
  }
}
