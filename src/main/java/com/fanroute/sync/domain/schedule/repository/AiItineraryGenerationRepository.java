package com.fanroute.sync.domain.schedule.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;

import jakarta.persistence.LockModeType;

public interface AiItineraryGenerationRepository extends JpaRepository<AiItineraryGeneration, Long> {

  Optional<AiItineraryGeneration> findByIdAndItineraryDayTripPlanUserId(Long generationId, Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select generation from AiItineraryGeneration generation where generation.id = :generationId")
  Optional<AiItineraryGeneration> findByIdForUpdate(@Param("generationId") Long generationId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("""
      update AiItineraryGeneration generation
      set generation.status = :processingStatus,
          generation.processingLeaseUntil = :leaseUntil,
          generation.nextAttemptAt = null,
          generation.attemptCount = generation.attemptCount + 1
      where generation.id = :generationId
        and ((generation.status = :pendingStatus
              and (generation.nextAttemptAt is null or generation.nextAttemptAt <= :now))
          or (generation.status = :processingStatus
              and generation.processingLeaseUntil <= :now))
      """)
  int acquireForProcessing(@Param("generationId") Long generationId,
      @Param("pendingStatus") AiItineraryGenerationStatus pendingStatus,
      @Param("processingStatus") AiItineraryGenerationStatus processingStatus,
      @Param("now") Instant now, @Param("leaseUntil") Instant leaseUntil);
}
