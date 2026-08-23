package com.fanroute.sync.domain.schedule.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;

public interface AiItineraryGenerationRepository extends JpaRepository<AiItineraryGeneration, Long> {

  Optional<AiItineraryGeneration> findByIdAndItineraryDayTripPlanUserId(Long generationId, Long userId);
}
