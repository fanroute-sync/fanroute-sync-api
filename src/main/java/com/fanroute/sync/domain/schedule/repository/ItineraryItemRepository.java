package com.fanroute.sync.domain.schedule.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.schedule.entity.ItineraryItem;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

  void deleteByItineraryDayTripPlanId(Long tripPlanId);

  @EntityGraph(attributePaths = {"place", "concert"})
  List<ItineraryItem> findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(Long itineraryDayId);

  @EntityGraph(attributePaths = "place")
  List<ItineraryItem> findByItineraryDayTripPlanIdAndPlaceIsNotNull(Long tripPlanId);

  Optional<ItineraryItem> findByIdAndItineraryDayTripPlanUserId(Long itemId, Long userId);
}
