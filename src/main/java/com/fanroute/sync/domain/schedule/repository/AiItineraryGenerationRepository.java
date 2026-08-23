package com.fanroute.sync.domain.schedule.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;

public interface AiItineraryGenerationRepository extends JpaRepository<AiItineraryGeneration, Long> {

  Optional<AiItineraryGeneration> findByIdAndItineraryDayTripPlanUserId(Long generationId, Long userId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("""
      update AiItineraryGeneration generation
      set generation.status = :processingStatus
      where generation.id = :generationId and generation.status = :pendingStatus
      """)
  int startIfPending(@Param("generationId") Long generationId,
      @Param("pendingStatus") AiItineraryGenerationStatus pendingStatus,
      @Param("processingStatus") AiItineraryGenerationStatus processingStatus);
}
