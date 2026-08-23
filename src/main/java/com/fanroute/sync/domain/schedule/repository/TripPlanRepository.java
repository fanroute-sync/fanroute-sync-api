package com.fanroute.sync.domain.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.schedule.entity.TripPlan;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {
}
