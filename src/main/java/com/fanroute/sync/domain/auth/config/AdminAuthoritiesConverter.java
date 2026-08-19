package com.fanroute.sync.domain.auth.config;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.UserService;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/** JWT subject의 사용자에게 저장된 ADMIN 권한을 Spring Security 권한으로 변환합니다. */
@Component
@RequiredArgsConstructor
public class AdminAuthoritiesConverter {

  private static final GrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

  private final UserService userService;

  public Collection<GrantedAuthority> resolveAuthorities(Jwt jwt) {
    Long userId = parseUserId(jwt.getSubject());
    if (userId == null) {
      return List.of();
    }

    try {
      User user = userService.findUser(userId);
      return user.isAdmin() ? List.of(ROLE_ADMIN) : List.of();
    } catch (BusinessException exception) {
      return List.of();
    }
  }

  private Long parseUserId(String subject) {
    try {
      return Long.valueOf(subject);
    } catch (NumberFormatException exception) {
      return null;
    }
  }
}
