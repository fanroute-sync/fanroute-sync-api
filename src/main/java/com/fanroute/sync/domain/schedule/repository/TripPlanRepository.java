package com.fanroute.sync.domain.schedule.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fanroute.sync.domain.schedule.entity.TripPlan;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {

  @EntityGraph(attributePaths = "concert")
  List<TripPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

  @EntityGraph(attributePaths = "concert")
  Optional<TripPlan> findByIdAndUserId(Long id, Long userId);

  @Modifying(flushAutomatically = true)
  @Query("""
      update TripPlan tripPlan
      set tripPlan.aiGenerationReservedCount = tripPlan.aiGenerationReservedCount + 1
      where tripPlan.id = :tripPlanId
        and tripPlan.aiGenerationUsedCount + tripPlan.aiGenerationReservedCount < :limit
      """)
  int reserveAiGeneration(@Param("tripPlanId") Long tripPlanId, @Param("limit") int limit);

  @Modifying(flushAutomatically = true)
  @Query("""
      update TripPlan tripPlan
      set tripPlan.aiGenerationReservedCount = tripPlan.aiGenerationReservedCount - 1,
          tripPlan.aiGenerationUsedCount = tripPlan.aiGenerationUsedCount + 1
      where tripPlan.id = :tripPlanId and tripPlan.aiGenerationReservedCount > 0
      """)
  int confirmAiGeneration(@Param("tripPlanId") Long tripPlanId);

  @Modifying(flushAutomatically = true)
  @Query("""
      update TripPlan tripPlan
      set tripPlan.aiGenerationReservedCount = tripPlan.aiGenerationReservedCount - 1
      where tripPlan.id = :tripPlanId and tripPlan.aiGenerationReservedCount > 0
      """)
  int releaseAiGeneration(@Param("tripPlanId") Long tripPlanId);
}
