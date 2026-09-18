package com.fanroute.sync.domain.concert.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.concert.entity.VenueItineraryTemplate;

public interface VenueItineraryTemplateRepository extends JpaRepository<VenueItineraryTemplate, Long> {

  Optional<VenueItineraryTemplate> findByVenueIdAndName(Long venueId, String name);

  List<VenueItineraryTemplate> findByVenueIdOrderByNameAsc(Long venueId);

  long countByVenueId(Long venueId);
}
