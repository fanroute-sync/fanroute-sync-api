package com.fanroute.sync.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.fanroute.sync.domain.auth.service.RefreshTokenService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.entity.vo.UserStatus;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.domain.user.repository.UserRepository;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private NicknameGenerator nicknameGenerator;

  @Mock
  private RefreshTokenService refreshTokenService;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, nicknameGenerator, refreshTokenService);
  }

  @Test
  @DisplayName("중복되지 않은 소셜 계정과 닉네임으로 사용자를 생성한다")
  void createUser() {
    User user = UserFixture.activeUser();
    when(nicknameGenerator.generate()).thenReturn("route");
    when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);

    User created = userService.createUser(
        AuthProvider.GOOGLE, "google-1", "user@example.com");

    assertSame(user, created);
    assertEquals("user@example.com", created.getEmail());
    verify(userRepository)
        .existsByAuthProviderAndProviderUserId(AuthProvider.GOOGLE, "google-1");
    verify(userRepository).existsByNickname("route");
    verify(userRepository).saveAndFlush(any(User.class));
  }

  @Test
  @DisplayName("동일한 소셜 계정은 중복 등록할 수 없다")
  void cannotCreateDuplicateSocialAccount() {
    when(userRepository.existsByAuthProviderAndProviderUserId(AuthProvider.APPLE, "apple-1"))
        .thenReturn(true);

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> userService.createUser(AuthProvider.APPLE, "apple-1"));

    assertEquals(UserErrorCode.USER_DUPLICATE_SOCIAL_ACCOUNT, exception.getErrorCode());
    verify(nicknameGenerator, never()).generate();
  }

  @Test
  @DisplayName("생성한 닉네임이 중복되면 새로운 닉네임을 생성한다")
  void retryNicknameGeneration() {
    User user = User.create("unique", AuthProvider.META, "meta-1");
    when(nicknameGenerator.generate()).thenReturn("duplicate", "unique");
    when(userRepository.existsByNickname("duplicate")).thenReturn(true);
    when(userRepository.existsByNickname("unique")).thenReturn(false);
    when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);

    User created = userService.createUser(AuthProvider.META, "meta-1");

    assertSame(user, created);
    verify(nicknameGenerator, times(2)).generate();
  }

  @Test
  @DisplayName("고유한 닉네임을 열 번 생성하지 못하면 예외가 발생한다")
  void failNicknameGeneration() {
    when(nicknameGenerator.generate()).thenReturn("duplicate");
    when(userRepository.existsByNickname("duplicate")).thenReturn(true);

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> userService.createUser(AuthProvider.GOOGLE, "google-1"));

    assertEquals(UserErrorCode.USER_NICKNAME_GENERATION_FAILED, exception.getErrorCode());
    verify(nicknameGenerator, times(10)).generate();
  }

  @Test
  @DisplayName("저장 시 무결성 충돌이 발생하면 가입 충돌 예외로 변환한다")
  void handleRegistrationConflict() {
    when(nicknameGenerator.generate()).thenReturn("route");
    when(userRepository.saveAndFlush(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("constraint violation"));

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> userService.createUser(AuthProvider.GOOGLE, "google-1"));

    assertEquals(UserErrorCode.USER_REGISTRATION_CONFLICT, exception.getErrorCode());
  }

  @Test
  @DisplayName("활성 사용자를 ID로 조회한다")
  void getAccessibleUser() {
    User user = UserFixture.activeUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    assertSame(user, userService.getAccessibleUser(1L));
  }

  @Test
  @DisplayName("존재하지 않는 사용자 ID를 조회하면 예외가 발생한다")
  void getMissingUser() {
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    BusinessException exception = assertThrows(BusinessException.class,
        () -> userService.findUser(1L));

    assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("정지 사용자는 접근할 수 없다")
  void cannotAccessSuspendedUser() {
    User user = UserFixture.activeUser();
    user.suspend();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    BusinessException exception = assertThrows(BusinessException.class,
        () -> userService.getAccessibleUser(1L));

    assertEquals(UserErrorCode.USER_SUSPENDED, exception.getErrorCode());
  }

  @Test
  @DisplayName("탈퇴 사용자는 접근할 수 없다")
  void cannotAccessWithdrawnUser() {
    User user = UserFixture.activeUserWithId(1L);
    user.withdraw(java.time.Instant.now());
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    BusinessException exception = assertThrows(BusinessException.class,
        () -> userService.getAccessibleUser(1L));

    assertEquals(UserErrorCode.USER_WITHDRAWN, exception.getErrorCode());
  }

  @Test
  @DisplayName("사용 중이지 않은 닉네임은 사용할 수 있다")
  void nicknameIsAvailable() {
    User user = UserFixture.activeUser();

    assertTrue(userService.isNicknameAvailable(user, " new-route "));
    verify(userRepository).existsByNickname("new-route");
  }

  @Test
  @DisplayName("다른 사용자가 사용 중인 닉네임은 사용할 수 없다")
  void nicknameIsUnavailable() {
    User user = UserFixture.activeUser();
    when(userRepository.existsByNickname("duplicate")).thenReturn(true);

    assertFalse(userService.isNicknameAvailable(user, "duplicate"));
  }

  @Test
  @DisplayName("현재 닉네임은 저장소 조회 없이 사용할 수 있다")
  void currentNicknameIsAvailable() {
    User user = UserFixture.activeUser();

    assertTrue(userService.isNicknameAvailable(user, " route "));
    verify(userRepository, never()).existsByNickname(any());
  }

  @Test
  @DisplayName("유효하지 않은 닉네임은 사용 가능 여부를 확인할 수 없다")
  void invalidNicknameCannotBeChecked() {
    User user = UserFixture.activeUser();

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> userService.isNicknameAvailable(user, "   "));

    assertEquals(UserErrorCode.USER_INVALID_NICKNAME, exception.getErrorCode());
  }

  @Test
  @DisplayName("동일한 닉네임으로 변경하면 저장소를 조회하지 않는다")
  void keepSameNickname() {
    User user = UserFixture.activeUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    User updated = userService.updateNickname(1L, " route ");

    assertSame(user, updated);
    verify(userRepository, never()).existsByNickname(any());
    verify(userRepository, never()).flush();
  }

  @Test
  @DisplayName("중복된 닉네임으로 변경할 수 없다")
  void cannotUpdateDuplicateNickname() {
    User user = UserFixture.activeUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.existsByNickname("duplicate")).thenReturn(true);

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> userService.updateNickname(1L, "duplicate"));

    assertEquals(UserErrorCode.USER_DUPLICATE_NICKNAME, exception.getErrorCode());
  }

  @Test
  @DisplayName("닉네임 변경 flush에서 발생한 충돌을 중복 예외로 변환한다")
  void handleNicknameUpdateConflict() {
    User user = UserFixture.activeUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    org.mockito.Mockito.doThrow(new DataIntegrityViolationException("constraint violation"))
        .when(userRepository).flush();

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> userService.updateNickname(1L, "new-route"));

    assertEquals(UserErrorCode.USER_DUPLICATE_NICKNAME, exception.getErrorCode());
  }

  @Test
  @DisplayName("사용자를 정지한 뒤 다시 활성화한다")
  void suspendAndActivateUser() {
    User user = UserFixture.activeUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    userService.suspendUser(1L);
    assertEquals(UserStatus.SUSPENDED, user.getStatus());

    userService.activateUser(1L);
    assertEquals(UserStatus.ACTIVE, user.getStatus());
  }

  @Test
  @DisplayName("사용자를 탈퇴 상태로 변경한다")
  void withdrawUser() {
    User user = UserFixture.activeUserWithId(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    userService.withdrawUser(1L);

    assertEquals(UserStatus.WITHDRAWN, user.getStatus());
    assertTrue(user.isDeleted());
    assertEquals("withdrawn_1", user.getNickname());
    verify(refreshTokenService).revokeAll(1L);
  }

  @Test
  @DisplayName("활성 사용자는 삭제 상태가 아니다")
  void activeUserIsNotDeleted() {
    User user = UserFixture.activeUser();

    assertFalse(user.isDeleted());
  }

  @Test
  @DisplayName("기존 소셜 사용자는 새로 생성하지 않고 로그인한다")
  void findsExistingSocialUserForLogin() {
    User user = UserFixture.activeUser();
    when(userRepository.findByAuthProviderAndProviderUserId(AuthProvider.GOOGLE, "google-1"))
        .thenReturn(Optional.of(user));

    UserService.SocialLoginResult result = userService.findOrCreateSocialUser(AuthProvider.GOOGLE,
        "google-1");

    assertSame(user, result.user());
    assertFalse(result.newUser());
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("최초 소셜 로그인 사용자는 기본 닉네임과 이메일로 생성한다")
  void createsUserOnFirstSocialLogin() {
    User user = UserFixture.activeUser();
    when(userRepository.findByAuthProviderAndProviderUserId(AuthProvider.GOOGLE, "google-1"))
        .thenReturn(Optional.empty());
    when(nicknameGenerator.generate()).thenReturn("route");
    when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);

    UserService.SocialLoginResult result = userService.findOrCreateSocialUser(
        AuthProvider.GOOGLE, "google-1", "user@example.com");

    assertSame(user, result.user());
    assertTrue(result.newUser());
    assertEquals("user@example.com", result.user().getEmail());
  }

  @Test
  @DisplayName("정지된 소셜 사용자는 로그인할 수 없다")
  void rejectsSuspendedSocialUserLogin() {
    User user = UserFixture.activeUser();
    user.suspend();
    when(userRepository.findByAuthProviderAndProviderUserId(AuthProvider.GOOGLE, "google-1"))
        .thenReturn(Optional.of(user));

    BusinessException exception = assertThrows(BusinessException.class,
        () -> userService.findOrCreateSocialUser(AuthProvider.GOOGLE, "google-1"));

    assertEquals(UserErrorCode.USER_SUSPENDED, exception.getErrorCode());
  }

  @Test
  @DisplayName("탈퇴한 소셜 사용자는 로그인하거나 재가입할 수 없다")
  void rejectsWithdrawnSocialUserLogin() {
    User user = UserFixture.activeUserWithId(1L);
    user.withdraw(java.time.Instant.now());
    when(userRepository.findByAuthProviderAndProviderUserId(AuthProvider.GOOGLE, "google-1"))
        .thenReturn(Optional.of(user));

    BusinessException exception = assertThrows(BusinessException.class,
        () -> userService.findOrCreateSocialUser(AuthProvider.GOOGLE, "google-1"));

    assertEquals(UserErrorCode.USER_WITHDRAWN, exception.getErrorCode());
  }
}