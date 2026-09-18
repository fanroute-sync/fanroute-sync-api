package com.fanroute.sync.domain.concert.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.concert.entity.RecommendationTemplatePlace;

public interface RecommendationTemplatePlaceRepository
    extends JpaRepository<RecommendationTemplatePlace, Long> {

  @EntityGraph(attributePaths = {"template", "recommendedPlace", "recommendedPlace.place"})
  List<RecommendationTemplatePlace> findByTemplateIdOrderBySortOrderAsc(Long templateId);

  void deleteByTemplateId(Long templateId);
}
