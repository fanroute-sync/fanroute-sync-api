package com.fanroute.sync.domain.auth.service;

import org.springframework.stereotype.Component;

import com.fanroute.sync.domain.user.service.SessionRevoker;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefreshTokenSessionRevoker implements SessionRevoker {

  private final RefreshTokenService refreshTokenService;

  @Override
  public void revokeAll(Long userId) {
    refreshTokenService.revokeAll(userId);
  }
}
