package com.fanroute.sync.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  @Modifying(flushAutomatically = true)
  @Query("""
      update User user
      set user.aiGenerationReservedCount = user.aiGenerationReservedCount + 1
      where user.id = :userId
        and user.aiGenerationUsedCount + user.aiGenerationReservedCount < :limit
      """)
  int reserveAiGeneration(@Param("userId") Long userId, @Param("limit") int limit);

  @Modifying(flushAutomatically = true)
  @Query("""
      update User user
      set user.aiGenerationReservedCount = user.aiGenerationReservedCount - 1,
          user.aiGenerationUsedCount = user.aiGenerationUsedCount + 1
      where user.id = :userId and user.aiGenerationReservedCount > 0
      """)
  int confirmAiGeneration(@Param("userId") Long userId);

  @Modifying(flushAutomatically = true)
  @Query("""
      update User user
      set user.aiGenerationReservedCount = user.aiGenerationReservedCount - 1
      where user.id = :userId and user.aiGenerationReservedCount > 0
      """)
  int releaseAiGeneration(@Param("userId") Long userId);
}
