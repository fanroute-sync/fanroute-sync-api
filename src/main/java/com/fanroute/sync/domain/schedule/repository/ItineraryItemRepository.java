package com.fanroute.sync.domain.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.schedule.entity.ItineraryItem;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

  void deleteByItineraryDayTripPlanId(Long tripPlanId);
}
