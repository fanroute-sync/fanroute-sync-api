package com.fanroute.sync.domain.concert.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.concert.entity.VenuePlaceCollection;

public interface VenuePlaceCollectionRepository extends JpaRepository<VenuePlaceCollection, Long> {

  Optional<VenuePlaceCollection> findByVenueIdAndName(Long venueId, String name);

  List<VenuePlaceCollection> findByVenueIdOrderByNameAsc(Long venueId);

  long countByVenueId(Long venueId);
}
