package com.fanroute.sync.domain.auth.service;

import org.springframework.stereotype.Service;

import com.fanroute.sync.domain.auth.dto.RefreshTokenDto;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenRefreshService {

  private final RefreshTokenService refreshTokenService;
  private final AccessTokenService accessTokenService;
  private final UserService userService;

  public RefreshTokenDto.Response refresh(String refreshToken) {
    Long userId = refreshTokenService.findUserId(refreshToken);
    User user = userService.getAccessibleUser(userId);
    AccessTokenService.IssuedToken newAccessToken = accessTokenService.issue(user);
    RefreshTokenService.IssuedToken newRefreshToken =
        refreshTokenService.rotate(refreshToken, userId);
    return new RefreshTokenDto.Response(
        newAccessToken.value(), newRefreshToken.value(), "Bearer",
        newAccessToken.expiresIn(), newRefreshToken.expiresIn());
  }
}
