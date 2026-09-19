package com.fanroute.sync.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fanroute.sync.domain.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
  Optional<Notification> findByIdAndUserId(Long id, Long userId);
  long countByUserIdAndReadAtIsNull(Long userId);
}
