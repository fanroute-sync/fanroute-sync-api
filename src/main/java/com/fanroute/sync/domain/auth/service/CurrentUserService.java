package com.fanroute.sync.domain.auth.service;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.domain.user.service.UserService;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurrentUserService implements CurrentUserResolver {

  private final UserService userService;

  /**
   * 인증된 요청의 JWT에서 현재 접근 가능한 사용자를 조회합니다.
   */
  @Override
  public User getCurrentUser(Jwt jwt) {
    Long userId = parseUserId(jwt.getSubject());
    return userService.getAccessibleUser(userId);
  }

  private Long parseUserId(String subject) {
    try {
      return Long.valueOf(subject);
    } catch (NumberFormatException exception) {
      throw new BusinessException(AuthErrorCode.ACCESS_TOKEN_INVALID);
    }
  }
}
