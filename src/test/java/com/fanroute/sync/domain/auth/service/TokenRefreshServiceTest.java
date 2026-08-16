package com.fanroute.sync.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.auth.dto.RefreshTokenDto;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.domain.user.service.UserService;
import com.fanroute.sync.global.common.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class TokenRefreshServiceTest {

  @Mock
  private RefreshTokenService refreshTokenService;
  @Mock
  private AccessTokenService accessTokenService;
  @Mock
  private UserService userService;
  @InjectMocks
  private TokenRefreshService service;

  @Test
  @DisplayName("Refresh Token을 회전하고 새로운 Access Token을 발급한다")
  void rotatesRefreshTokenAndIssuesAccessToken() {
    User user = User.create("route", AuthProvider.GOOGLE, "google-sub");
    ReflectionTestUtils.setField(user, "id", 1L);
    when(refreshTokenService.findUserId("old-token")).thenReturn(1L);
    when(userService.getAccessibleUser(1L)).thenReturn(user);
    when(refreshTokenService.rotate("old-token", 1L))
        .thenReturn(new RefreshTokenService.IssuedToken("new-refresh-token", 1209600));
    when(accessTokenService.issue(user))
        .thenReturn(new AccessTokenService.IssuedToken("new-access-token", 3600));

    RefreshTokenDto.Response response = service.refresh("old-token");

    assertThat(response.accessToken()).isEqualTo("new-access-token");
    assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    InOrder order = inOrder(refreshTokenService, userService, accessTokenService);
    order.verify(refreshTokenService).findUserId("old-token");
    order.verify(userService).getAccessibleUser(1L);
    order.verify(accessTokenService).issue(user);
    order.verify(refreshTokenService).rotate("old-token", 1L);
  }

  @Test
  @DisplayName("정지 사용자의 토큰 재발급을 차단한다")
  void rejectsSuspendedUserBeforeIssuingTokens() {
    assertUserStatusBlocksRefresh(UserErrorCode.USER_SUSPENDED);
  }

  @Test
  @DisplayName("탈퇴 사용자의 토큰 재발급을 차단한다")
  void rejectsWithdrawnUserBeforeIssuingTokens() {
    assertUserStatusBlocksRefresh(UserErrorCode.USER_WITHDRAWN);
  }

  private void assertUserStatusBlocksRefresh(UserErrorCode errorCode) {
    when(refreshTokenService.findUserId("old-token")).thenReturn(1L);
    when(userService.getAccessibleUser(1L)).thenThrow(new BusinessException(errorCode));

    assertThatThrownBy(() -> service.refresh("old-token"))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));

    verifyNoInteractions(accessTokenService);
    org.mockito.Mockito.verify(refreshTokenService, org.mockito.Mockito.never())
        .rotate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
  }
}
