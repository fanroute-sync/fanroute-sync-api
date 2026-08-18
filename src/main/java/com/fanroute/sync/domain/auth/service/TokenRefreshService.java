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

  public RefreshResult refresh(String refreshToken) {
    Long userId = refreshTokenService.findUserId(refreshToken);
    User user = userService.getAccessibleUser(userId);
    AccessTokenService.IssuedToken newAccessToken =
        accessTokenService.issue(AuthPrincipal.from(user));
    RefreshTokenService.IssuedToken newRefreshToken =
        refreshTokenService.rotate(refreshToken, userId);
    RefreshTokenDto.Response response = new RefreshTokenDto.Response(
        newAccessToken.value(), "Bearer", newAccessToken.expiresIn());
    return new RefreshResult(response, newRefreshToken);
  }

  public record RefreshResult(
      RefreshTokenDto.Response response, RefreshTokenService.IssuedToken refreshToken) {

  }
}
