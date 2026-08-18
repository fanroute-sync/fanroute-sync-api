package com.fanroute.sync.domain.auth.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenSessionRevokerTest {

  @Mock
  private RefreshTokenService refreshTokenService;

  @Test
  @DisplayName("사용자의 모든 Refresh Token 폐기를 RefreshTokenService에 위임한다")
  void delegatesRevokeAllToRefreshTokenService() {
    RefreshTokenSessionRevoker revoker = new RefreshTokenSessionRevoker(refreshTokenService);

    revoker.revokeAll(1L);

    verify(refreshTokenService).revokeAll(1L);
  }
}
