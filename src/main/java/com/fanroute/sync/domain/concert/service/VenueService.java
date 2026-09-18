package com.fanroute.sync.domain.concert.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.domain.concert.repository.VenuePlaceCollectionRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueService {

  private final VenueRepository venueRepository;
  private final ConcertRepository concertRepository;
  private final VenuePlaceCollectionRepository collectionRepository;
  private final Clock clock;

  public Page<Venue> getVenues(Pageable pageable) {
    return venueRepository.findAll(pageable);
  }

  public Venue getVenue(Long venueId) {
    return venueRepository.findById(venueId)
        .orElseThrow(() -> new BusinessException(ConcertErrorCode.VENUE_NOT_FOUND));
  }

  public long countUpcomingConcerts(Long venueId) {
    return concertRepository.countByVenueIdAndEndDateGreaterThanEqual(venueId, LocalDate.now(clock));
  }

  public long countPlaceCollections(Long venueId) {
    return collectionRepository.countByVenueId(venueId);
  }
}
