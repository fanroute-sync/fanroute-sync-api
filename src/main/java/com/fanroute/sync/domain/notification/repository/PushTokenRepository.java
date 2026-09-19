package com.fanroute.sync.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.fanroute.sync.domain.notification.entity.PushToken;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {
  Optional<PushToken> findByToken(String token);
  List<PushToken> findByUserId(Long userId);
  void deleteByUserIdAndToken(Long userId, String token);
}
