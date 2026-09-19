package com.fanroute.sync.domain.concert.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.concert.entity.VenuePlaceCollectionItem;

public interface VenuePlaceCollectionItemRepository
    extends JpaRepository<VenuePlaceCollectionItem, Long> {

  @EntityGraph(attributePaths = {"place", "place.tags"})
  List<VenuePlaceCollectionItem> findByCollectionIdOrderBySortOrderAsc(Long collectionId);

  void deleteByCollectionId(Long collectionId);
}
