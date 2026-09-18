package com.fanroute.sync.domain.concert.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.concert.entity.VenueRecommendedPlace;

public interface VenueRecommendedPlaceRepository
    extends JpaRepository<VenueRecommendedPlace, Long> {

  @EntityGraph(attributePaths = {"venue", "place"})
  List<VenueRecommendedPlace> findByVenueIdOrderBySortOrderAsc(Long venueId);
}
