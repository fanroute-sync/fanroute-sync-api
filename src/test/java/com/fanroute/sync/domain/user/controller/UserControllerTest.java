package com.fanroute.sync.domain.user.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.domain.user.service.SessionCookieClearer;
import com.fanroute.sync.domain.user.service.UserService;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.config.SecurityConfig;
import com.fanroute.sync.support.SecurityWebMvcTestSupport;
import com.fanroute.sync.support.UserFixture;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, SecurityWebMvcTestSupport.class})
class UserControllerTest {

  private static final String CLEARED_COOKIE_HEADER =
      "refreshToken=; Path=/api/v1/auth; Max-Age=0; Secure; HttpOnly; SameSite=Lax";

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private CurrentUserResolver currentUserResolver;
  @MockitoBean
  private UserService userService;
  @MockitoBean
  private SessionCookieClearer sessionCookieClearer;

  private User user;

  @BeforeEach
  void setUp() {
    user = UserFixture.activeUserWithId(1L);
  }

  @Test
  @DisplayName("현재 사용자의 프로필을 조회하고 소셜 계정 식별자는 노출하지 않는다")
  void getsMyProfileWithoutProviderUserId() throws Exception {
    when(currentUserResolver.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenReturn(user);

    mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(jwt -> jwt.subject("1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.nickname").value("route"))
        .andExpect(jsonPath("$.data.email").value("user@example.com"))
        .andExpect(jsonPath("$.data.authProvider").value("GOOGLE"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.providerUserId").doesNotExist());
  }

  @Test
  @DisplayName("사용 가능한 닉네임을 정규화하여 반환한다")
  void getsNicknameAvailability() throws Exception {
    when(currentUserResolver.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenReturn(user);
    when(userService.isNicknameAvailable(user, "new-route")).thenReturn(true);

    mockMvc.perform(get("/api/v1/users/nickname-availability")
            .param("nickname", " new-route ")
            .with(jwt().jwt(jwt -> jwt.subject("1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nickname").value("new-route"))
        .andExpect(jsonPath("$.data.available").value(true));
  }

  @Test
  @DisplayName("중복된 닉네임은 정상 응답에서 사용할 수 없음으로 반환한다")
  void getsUnavailableNickname() throws Exception {
    when(currentUserResolver.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenReturn(user);
    when(userService.isNicknameAvailable(user, "duplicate")).thenReturn(false);

    mockMvc.perform(get("/api/v1/users/nickname-availability")
            .param("nickname", "duplicate")
            .with(jwt().jwt(jwt -> jwt.subject("1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.available").value(false));
  }

  @Test
  @DisplayName("현재 닉네임은 사용할 수 있는 것으로 반환한다")
  void getsCurrentNicknameAsAvailable() throws Exception {
    when(currentUserResolver.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenReturn(user);
    when(userService.isNicknameAvailable(user, "route")).thenReturn(true);

    mockMvc.perform(get("/api/v1/users/nickname-availability")
            .param("nickname", "route")
            .with(jwt().jwt(jwt -> jwt.subject("1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nickname").value("route"))
        .andExpect(jsonPath("$.data.available").value(true));
  }

  @Test
  @DisplayName("유효하지 않은 닉네임 조회는 사용자 입력 오류를 반환한다")
  void rejectsInvalidNicknameAvailability() throws Exception {
    when(currentUserResolver.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenReturn(user);

    mockMvc.perform(get("/api/v1/users/nickname-availability")
            .param("nickname", "   ")
            .with(jwt().jwt(jwt -> jwt.subject("1"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_INVALID_NICKNAME"));
  }

  @Test
  @DisplayName("현재 사용자의 닉네임을 수정하고 변경된 프로필을 반환한다")
  void updatesMyProfile() throws Exception {
    User updated = UserFixture.activeUserWithId(1L);
    updated.updateNickname("new-route");
    when(currentUserResolver.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenReturn(user);
    when(userService.updateNickname(1L, "new-route")).thenReturn(updated);

    mockMvc.perform(patch("/api/v1/users/me")
            .with(jwt().jwt(jwt -> jwt.subject("1")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"new-route\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nickname").value("new-route"))
        .andExpect(jsonPath("$.data.providerUserId").doesNotExist());
  }

  @Test
  @DisplayName("현재 닉네임으로 수정하면 동일한 프로필을 반환한다")
  void updatesSameNicknameIdempotently() throws Exception {
    when(currentUserResolver.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenReturn(user);
    when(userService.updateNickname(1L, "route")).thenReturn(user);

    mockMvc.perform(patch("/api/v1/users/me")
            .with(jwt().jwt(jwt -> jwt.subject("1")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"route\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.nickname").value("route"));
  }

  @Test
  @DisplayName("유효하지 않은 닉네임으로 프로필을 수정할 수 없다")
  void rejectsInvalidNicknameUpdate() throws Exception {
    when(currentUserResolver.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenReturn(user);
    when(userService.updateNickname(1L, "   "))
        .thenThrow(new BusinessException(UserErrorCode.USER_INVALID_NICKNAME));

    mockMvc.perform(patch("/api/v1/users/me")
            .with(jwt().jwt(jwt -> jwt.subject("1")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_INVALID_NICKNAME"));
  }

  @Test
  @DisplayName("닉네임 동시 변경 충돌은 409 응답을 반환한다")
  void returnsConflictForDuplicateNickname() throws Exception {
    when(currentUserResolver.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenReturn(user);
    when(userService.updateNickname(1L, "duplicate"))
        .thenThrow(new BusinessException(UserErrorCode.USER_DUPLICATE_NICKNAME));

    mockMvc.perform(patch("/api/v1/users/me")
            .with(jwt().jwt(jwt -> jwt.subject("1")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"duplicate\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_DUPLICATE_NICKNAME"));
  }

  @Test
  @DisplayName("정지 사용자는 프로필을 조회할 수 없다")
  void rejectsSuspendedUser() throws Exception {
    when(currentUserResolver.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new BusinessException(UserErrorCode.USER_SUSPENDED));

    mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(jwt -> jwt.subject("1"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("USER_SUSPENDED"));
  }

  @Test
  @DisplayName("탈퇴 사용자는 프로필을 수정할 수 없다")
  void rejectsWithdrawnUserUpdate() throws Exception {
    when(currentUserResolver.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new BusinessException(UserErrorCode.USER_WITHDRAWN));

    mockMvc.perform(patch("/api/v1/users/me")
            .with(jwt().jwt(jwt -> jwt.subject("1")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"new-route\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("USER_WITHDRAWN"));
  }

  @Test
  @DisplayName("인증되지 않은 사용자는 프로필을 조회할 수 없다")
  void rejectsUnauthenticatedUser() throws Exception {
    mockMvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
  }

  @Test
  @DisplayName("현재 사용자를 회원 탈퇴 처리한다")
  void withdrawsCurrentUser() throws Exception {
    when(currentUserResolver.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenReturn(user);
    when(sessionCookieClearer.clear()).thenReturn(CLEARED_COOKIE_HEADER);

    mockMvc.perform(delete("/api/v1/users/me")
            .with(jwt().jwt(jwt -> jwt.subject("1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
        .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")));

    verify(userService).withdrawUser(1L);
  }
}
