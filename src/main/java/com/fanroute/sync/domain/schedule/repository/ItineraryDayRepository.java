package com.fanroute.sync.domain.schedule.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.schedule.entity.ItineraryDay;

public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {

  List<ItineraryDay> findByTripPlanIdOrderByDateAsc(Long tripPlanId);

  void deleteByTripPlanId(Long tripPlanId);
}
