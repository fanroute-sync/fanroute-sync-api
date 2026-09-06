package com.fanroute.sync.domain.schedule.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.fanroute.sync.domain.schedule.entity.AiGenerationOutboxEvent;
import com.fanroute.sync.domain.schedule.entity.AiGenerationOutboxStatus;

import jakarta.persistence.LockModeType;

public interface AiGenerationOutboxEventRepository
    extends JpaRepository<AiGenerationOutboxEvent, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select event from AiGenerationOutboxEvent event
      where (event.status = :pendingStatus and event.nextAttemptAt <= :now)
         or (event.status = :publishingStatus and event.publishLeaseUntil <= :now)
      order by event.id
      """)
  List<AiGenerationOutboxEvent> lockDispatchable(Instant now,
      AiGenerationOutboxStatus pendingStatus, AiGenerationOutboxStatus publishingStatus,
      Pageable pageable);
}
