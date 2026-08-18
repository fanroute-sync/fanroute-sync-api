package com.fanroute.sync.domain.auth.service;

import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;

public record AuthPrincipal(Long userId, AuthProvider authProvider) {

  public static AuthPrincipal from(User user) {
    return new AuthPrincipal(user.getId(), user.getAuthProvider());
  }
}
