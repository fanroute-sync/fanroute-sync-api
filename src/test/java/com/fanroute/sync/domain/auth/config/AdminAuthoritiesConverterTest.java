package com.fanroute.sync.domain.auth.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.UserRole;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.domain.user.service.UserService;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class AdminAuthoritiesConverterTest {

  @Mock
  private UserService userService;

  @Mock
  private Jwt jwt;

  private AdminAuthoritiesConverter converter;

  @BeforeEach
  void setUp() {
    converter = new AdminAuthoritiesConverter(userService);
  }

  @Test
  @DisplayName("ADMIN 사용자에게는 ROLE_ADMIN 권한을 부여한다")
  void grantsAdminAuthorityForAdminUser() {
    User admin = UserFixture.activeUserWithId(1L);
    admin.updateRole(UserRole.ADMIN);
    when(jwt.getSubject()).thenReturn("1");
    when(userService.findUser(1L)).thenReturn(admin);

    Collection<GrantedAuthority> authorities = converter.resolveAuthorities(jwt);

    assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
  }

  @Test
  @DisplayName("일반 사용자에게는 권한을 부여하지 않는다")
  void grantsNoAuthorityForRegularUser() {
    User user = UserFixture.activeUserWithId(1L);
    when(jwt.getSubject()).thenReturn("1");
    when(userService.findUser(1L)).thenReturn(user);

    assertTrue(converter.resolveAuthorities(jwt).isEmpty());
  }

  @Test
  @DisplayName("subject가 숫자가 아니면 권한을 부여하지 않는다")
  void grantsNoAuthorityForInvalidSubject() {
    when(jwt.getSubject()).thenReturn("not-a-number");

    assertTrue(converter.resolveAuthorities(jwt).isEmpty());
  }

  @Test
  @DisplayName("사용자를 찾을 수 없으면 권한을 부여하지 않는다")
  void grantsNoAuthorityWhenUserNotFound() {
    when(jwt.getSubject()).thenReturn("999");
    when(userService.findUser(999L)).thenThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

    assertTrue(converter.resolveAuthorities(jwt).isEmpty());
  }
}
