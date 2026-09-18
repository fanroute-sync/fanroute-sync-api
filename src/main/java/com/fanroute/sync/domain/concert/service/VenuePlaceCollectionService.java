package com.fanroute.sync.domain.concert.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.concert.dto.VenuePlaceCollectionDto;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.entity.VenuePlaceCollection;
import com.fanroute.sync.domain.concert.entity.VenuePlaceCollectionItem;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.VenuePlaceCollectionItemRepository;
import com.fanroute.sync.domain.concert.repository.VenuePlaceCollectionRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.service.PlaceService;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenuePlaceCollectionService {

  private final VenuePlaceCollectionRepository collectionRepository;
  private final VenuePlaceCollectionItemRepository itemRepository;
  private final VenueRepository venueRepository;
  private final PlaceService placeService;

  public List<VenuePlaceCollectionDto.Response> getCollections(Long venueId) {
    if (!venueRepository.existsById(venueId)) {
      throw new BusinessException(ConcertErrorCode.VENUE_NOT_FOUND);
    }
    return collectionRepository.findByVenueIdOrderByNameAsc(venueId).stream()
        .map(collection -> VenuePlaceCollectionDto.Response.from(collection,
            itemRepository.findByCollectionIdOrderBySortOrderAsc(collection.getId())))
        .toList();
  }

  @Transactional
  public VenuePlaceCollectionDto.Response importCollection(
      VenuePlaceCollectionDto.ImportRequest request) {
    Venue venue = venueRepository.findById(request.venueId())
        .orElseThrow(() -> new BusinessException(ConcertErrorCode.VENUE_NOT_FOUND));
    VenuePlaceCollection collection = collectionRepository
        .findByVenueIdAndName(request.venueId(), request.name())
        .orElseGet(() -> collectionRepository.save(
            VenuePlaceCollection.create(venue, request.name(), request.description(), request.type())));
    collection.update(request.description(), request.type());

    itemRepository.deleteByCollectionId(collection.getId());
    itemRepository.flush();
    List<VenuePlaceCollectionItem> items = request.items().stream()
        .map(item -> VenuePlaceCollectionItem.create(collection,
            placeService.getPlace(item.placeId()), item.sortOrder()))
        .toList();
    itemRepository.saveAll(items);
    return VenuePlaceCollectionDto.Response.from(collection,
        itemRepository.findByCollectionIdOrderBySortOrderAsc(collection.getId()));
  }
}
