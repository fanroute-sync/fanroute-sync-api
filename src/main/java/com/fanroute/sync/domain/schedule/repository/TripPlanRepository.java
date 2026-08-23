package com.fanroute.sync.domain.schedule.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.schedule.entity.TripPlan;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {

  List<TripPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

  Optional<TripPlan> findByIdAndUserId(Long id, Long userId);
}
