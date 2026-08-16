package com.fanroute.sync.domain.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import com.fanroute.sync.domain.auth.client.GoogleTokenClient;
import com.fanroute.sync.domain.auth.config.AuthProperties;
import com.fanroute.sync.domain.auth.dto.GoogleOAuthDto;
import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.service.UserService;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.external.ExternalApiErrorType;
import com.fanroute.sync.global.external.ExternalApiException;

import lombok.RequiredArgsConstructor;

/**
 * Google 코드 교환부터 서비스 사용자 연동과 Access Token 발급까지 로그인 흐름을 조정합니다. Google Access
 * Token 자체는 사용자 식별에 사용하지
 * 않고, 검증된 ID Token의 {@code sub}를 사용합니다.
 */
@Service
@RequiredArgsConstructor
public class GoogleLoginService {

  private final GoogleTokenClient googleTokenClient;
  private final GoogleIdTokenVerifier idTokenVerifier;
  private final UserService userService;
  private final AccessTokenService accessTokenService;
  private final RefreshTokenService refreshTokenService;
  private final AuthProperties properties;

  /**
   * Google 인가 코드로 사용자를 로그인하고 서비스 Access Token을 반환합니다.
   */
  public LoginDto.Response login(String authorizationCode) {
    GoogleOAuthDto.TokenResponse tokens = exchangeToken(authorizationCode);
    GoogleIdTokenVerifier.GoogleUserInfo googleUser =
        idTokenVerifier.verifyAndExtractUserInfo(tokens.idToken());
    UserService.SocialLoginResult result = userService.findOrCreateSocialUser(AuthProvider.GOOGLE,
        googleUser.subject(), googleUser.email());
    LoginDto.Response response = accessTokenService.issue(result.user(), result.newUser());
    RefreshTokenService.IssuedToken refreshToken = refreshTokenService.issue(result.user().getId());
    return response.withRefreshToken(refreshToken.value(), refreshToken.expiresIn());
  }

  private GoogleOAuthDto.TokenResponse exchangeToken(String code) {
    MultiValueMap<String, String> requestForm = properties.google().toRequestForm(code);

    try {
      GoogleOAuthDto.TokenResponse response = googleTokenClient.exchangeToken(requestForm);
      if (response == null || response.hasInvalidIdToken()) {
        throw new BusinessException(AuthErrorCode.GOOGLE_ID_TOKEN_INVALID);
      }
      return response;
    } catch (ExternalApiException exception) {
      throw handleExternalApiException(exception);
    }

  }

  private BusinessException handleExternalApiException(final ExternalApiException exception) {
    if (exception.getErrorType() == ExternalApiErrorType.CLIENT_ERROR) {
      return new BusinessException(AuthErrorCode.GOOGLE_AUTHORIZATION_CODE_INVALID);
    }
    return new BusinessException(AuthErrorCode.GOOGLE_API_UNAVAILABLE);
  }
}
