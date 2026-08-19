package com.fanroute.sync.domain.concert.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

  private final ConcertRepository concertRepository;
  private final Clock clock;

  public Page<Concert> getConcerts(Genre genreName, Pageable pageable) {
    LocalDate today = LocalDate.now(clock);
    if (genreName == null) {
      return concertRepository.findByEndDateGreaterThanEqual(today, pageable);
    }
    return concertRepository.findByEndDateGreaterThanEqualAndGenreName(
        today, genreName, pageable);
  }

  public Concert getConcert(Long concertId) {
    return concertRepository.findWithVenueById(concertId)
        .orElseThrow(() -> new BusinessException(ConcertErrorCode.CONCERT_NOT_FOUND));
  }
}
