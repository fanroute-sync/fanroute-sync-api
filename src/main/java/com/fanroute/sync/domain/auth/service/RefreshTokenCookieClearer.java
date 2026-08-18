package com.fanroute.sync.domain.auth.service;

import org.springframework.stereotype.Component;

import com.fanroute.sync.domain.auth.controller.RefreshTokenCookie;
import com.fanroute.sync.domain.user.service.SessionCookieClearer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieClearer implements SessionCookieClearer {

  private final RefreshTokenCookie refreshTokenCookie;

  @Override
  public String clear() {
    return refreshTokenCookie.clear().toString();
  }
}
