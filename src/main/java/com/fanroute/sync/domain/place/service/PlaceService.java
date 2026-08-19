package com.fanroute.sync.domain.place.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.domain.place.repository.PlaceRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

  private final PlaceRepository placeRepository;

  public Page<Place> getPlaces(PlaceCategory category, Pageable pageable) {
    if (category == null) {
      return placeRepository.findAll(pageable);
    }
    return placeRepository.findByCategory(category, pageable);
  }

  public Place getPlace(Long placeId) {
    return placeRepository.findById(placeId)
        .orElseThrow(() -> new BusinessException(PlaceErrorCode.PLACE_NOT_FOUND));
  }
}
