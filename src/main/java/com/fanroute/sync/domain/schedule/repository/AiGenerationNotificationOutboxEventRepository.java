package com.fanroute.sync.domain.schedule.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.fanroute.sync.domain.schedule.entity.AiGenerationNotificationOutboxEvent;
import com.fanroute.sync.domain.schedule.entity.AiGenerationNotificationStatus;
import com.fanroute.sync.domain.schedule.entity.AiGenerationNotificationType;

import jakarta.persistence.LockModeType;

public interface AiGenerationNotificationOutboxEventRepository
    extends JpaRepository<AiGenerationNotificationOutboxEvent, Long> {

  long countByGenerationIdAndType(Long generationId, AiGenerationNotificationType type);

  long countByGenerationId(Long generationId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select event from AiGenerationNotificationOutboxEvent event
      where (event.status = :pendingStatus and event.nextAttemptAt <= :now)
         or (event.status = :processingStatus and event.processingLeaseUntil <= :now)
      order by event.id
      """)
  List<AiGenerationNotificationOutboxEvent> lockDispatchable(
      Instant now,
      AiGenerationNotificationStatus pendingStatus,
      AiGenerationNotificationStatus processingStatus,
      Pageable pageable);
}
