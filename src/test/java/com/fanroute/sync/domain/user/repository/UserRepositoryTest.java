package com.fanroute.sync.domain.user.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.entity.vo.UserStatus;
import com.fanroute.sync.support.AbstractRepositoryTest;

class UserRepositoryTest extends AbstractRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("ID와 상태로 사용자를 조회한다")
  void findByIdAndStatus() {
    User saved = userRepository.saveAndFlush(
        User.create(
            "route",
            AuthProvider.GOOGLE,
            "google-1"
        )
    );

    assertTrue(
        userRepository
            .findByIdAndStatus(saved.getId(), UserStatus.ACTIVE)
            .isPresent()
    );

    assertFalse(
        userRepository
            .findByIdAndStatus(saved.getId(), UserStatus.SUSPENDED)
            .isPresent()
    );

    assertNotNull(saved.getCreatedAt());
    assertNotNull(saved.getUpdatedAt());
  }

  @Test
  @DisplayName("닉네임 중복 여부를 조회한다")
  void existsByNickname() {
    userRepository.saveAndFlush(
        User.create(
            "route",
            AuthProvider.APPLE,
            "apple-1"
        )
    );

    assertTrue(userRepository.existsByNickname("route"));
    assertFalse(userRepository.existsByNickname("another"));
  }

  @Test
  @DisplayName("소셜 로그인 제공자와 제공자 사용자 ID로 조회한다")
  void findBySocialAccount() {
    userRepository.saveAndFlush(
        User.create(
            "route",
            AuthProvider.META,
            "meta-1"
        )
    );

    User found = userRepository
        .findByAuthProviderAndProviderUserId(
            AuthProvider.META,
            "meta-1"
        )
        .orElseThrow();

    assertEquals("route", found.getNickname());
  }

  @Test
  @DisplayName("소셜 로그인 제공자와 사용자 ID의 존재 여부를 조회한다")
  void existsBySocialAccount() {
    userRepository.saveAndFlush(
        User.create("route", AuthProvider.GOOGLE, "provider-user-1"));

    assertTrue(userRepository.existsByAuthProviderAndProviderUserId(
        AuthProvider.GOOGLE, "provider-user-1"));
    assertFalse(userRepository.existsByAuthProviderAndProviderUserId(
        AuthProvider.APPLE, "provider-user-1"));
    assertFalse(userRepository.existsByAuthProviderAndProviderUserId(
        AuthProvider.GOOGLE, "missing"));
  }

  @Test
  @DisplayName("소셜 로그인 식별자와 사용자 상태로 조회한다")
  void findBySocialAccountAndStatus() {
    User user = User.create(
        "route",
        AuthProvider.GOOGLE,
        "google-1"
    );
    user.suspend();

    userRepository.saveAndFlush(user);

    assertTrue(
        userRepository
            .findByAuthProviderAndProviderUserIdAndStatus(
                AuthProvider.GOOGLE,
                "google-1",
                UserStatus.SUSPENDED
            )
            .isPresent()
    );

    assertFalse(
        userRepository
            .findByAuthProviderAndProviderUserIdAndStatus(
                AuthProvider.GOOGLE,
                "google-1",
                UserStatus.ACTIVE
            )
            .isPresent()
    );
  }

  @Test
  @DisplayName("탈퇴하지 않은 사용자끼리는 동일한 닉네임을 사용할 수 없다")
  void cannotSaveDuplicateNickname() {
    userRepository.saveAndFlush(
        User.create(
            "route",
            AuthProvider.GOOGLE,
            "google-1"
        )
    );

    assertThrows(
        DataIntegrityViolationException.class,
        () -> userRepository.saveAndFlush(
            User.create(
                "route",
                AuthProvider.APPLE,
                "apple-1"
            )
        )
    );
  }

  @Test
  @DisplayName("동일한 소셜 로그인 제공자와 사용자 ID는 중복 저장할 수 없다")
  void cannotSaveDuplicateSocialAccount() {
    userRepository.saveAndFlush(
        User.create(
            "route-one",
            AuthProvider.GOOGLE,
            "google-1"
        )
    );

    assertThrows(
        DataIntegrityViolationException.class,
        () -> userRepository.saveAndFlush(
            User.create(
                "route-two",
                AuthProvider.GOOGLE,
                "google-1"
            )
        )
    );
  }

  @Test
  @DisplayName("제공자가 다르면 동일한 제공자 사용자 ID를 사용할 수 있다")
  void canSaveSameProviderUserIdWithDifferentProvider() {
    userRepository.saveAndFlush(
        User.create(
            "route-one",
            AuthProvider.GOOGLE,
            "provider-user-1"
        )
    );

    assertDoesNotThrow(
        () -> userRepository.saveAndFlush(
            User.create(
                "route-two",
                AuthProvider.APPLE,
                "provider-user-1"
            )
        )
    );
  }

  @Test
  @DisplayName("탈퇴하면 기존 닉네임은 해제되고 소셜 계정 식별자는 유지된다")
  void keepSocialAccountAfterWithdrawal() {
    User user = userRepository.saveAndFlush(
        User.create("route", AuthProvider.GOOGLE, "google-1"));

    user.withdraw(Instant.parse("2026-07-17T00:00:00Z"));
    userRepository.flush();

    assertFalse(userRepository.existsByNickname("route"));
    assertTrue(userRepository.existsByNickname("withdrawn_" + user.getId()));
    assertTrue(userRepository.existsByAuthProviderAndProviderUserId(
        AuthProvider.GOOGLE, "google-1"));
    assertTrue(userRepository.findByAuthProviderAndProviderUserIdAndStatus(
        AuthProvider.GOOGLE, "google-1", UserStatus.WITHDRAWN).isPresent());
  }
}
