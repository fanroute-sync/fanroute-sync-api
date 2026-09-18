package com.fanroute.sync.domain.concert.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.concert.entity.RecommendationTemplate;

public interface RecommendationTemplateRepository extends JpaRepository<RecommendationTemplate, Long> {

  @EntityGraph(attributePaths = "venue")
  List<RecommendationTemplate> findByVenueIdOrderByIdAsc(Long venueId);
}
