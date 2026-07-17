package com.fanroute.sync.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.entity.vo.UserStatus;

public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByNickname(String nickname);

  boolean existsByAuthProviderAndProviderUserId(
      AuthProvider authProvider, String providerUserId);

  Optional<User> findByAuthProviderAndProviderUserId(
      AuthProvider authProvider, String providerUserId);

  Optional<User> findByIdAndStatus(Long id, UserStatus status);

  Optional<User> findByAuthProviderAndProviderUserIdAndStatus(
      AuthProvider authProvider, String providerUserId, UserStatus status);
}
