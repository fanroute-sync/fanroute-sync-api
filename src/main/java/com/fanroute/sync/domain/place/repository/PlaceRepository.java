package com.fanroute.sync.domain.place.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;

public interface PlaceRepository extends JpaRepository<Place, Long> {

  Optional<Place> findByContentId(String contentId);

  Page<Place> findByCategory(PlaceCategory category, Pageable pageable);
}
