package com.fanroute.sync.domain.concert.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.concert.entity.VenueItineraryTemplateItem;

public interface VenueItineraryTemplateItemRepository
    extends JpaRepository<VenueItineraryTemplateItem, Long> {

  @EntityGraph(attributePaths = {"place", "place.tags"})
  List<VenueItineraryTemplateItem> findByTemplateIdOrderBySortOrderAsc(Long templateId);

  void deleteByTemplateId(Long templateId);
}
