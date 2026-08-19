package com.fanroute.sync.domain.concert.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.support.AbstractRepositoryTest;

class ConcertRepositoryTest extends AbstractRepositoryTest {

  @Autowired
  private ConcertRepository concertRepository;
  @Autowired
  private VenueRepository venueRepository;

  @Test
  @DisplayName("ID로 조회한 공연의 공연장은 이미 초기화되어 있다")
  void findWithVenueByIdInitializesVenue() {
    Concert saved = concertRepository.saveAndFlush(createConcert());

    Concert found = concertRepository.findWithVenueById(saved.getId()).orElseThrow();

    assertThat(Hibernate.isInitialized(found.getVenue())).isTrue();
    assertThat(found.getVenue().getName()).isEqualTo("테스트홀");
  }

  @Test
  @DisplayName("목록 조회한 공연의 공연장도 이미 초기화되어 있다")
  void findByEndDateGreaterThanEqualInitializesVenue() {
    concertRepository.saveAndFlush(createConcert());

    Page<Concert> page = concertRepository.findByEndDateGreaterThanEqual(
        LocalDate.of(2026, 1, 1), PageRequest.of(0, 20));

    assertThat(page.getContent()).isNotEmpty();
    Concert found = page.getContent().get(0);
    assertThat(Hibernate.isInitialized(found.getVenue())).isTrue();
    assertThat(found.getVenue().getName()).isEqualTo("테스트홀");
  }

  @Test
  @DisplayName("장르로 필터링한 목록 조회의 공연장도 이미 초기화되어 있다")
  void findByEndDateGreaterThanEqualAndGenreNameInitializesVenue() {
    concertRepository.saveAndFlush(createConcert());

    Page<Concert> page = concertRepository.findByEndDateGreaterThanEqualAndGenreName(
        LocalDate.of(2026, 1, 1), Genre.POPULAR_MUSIC, PageRequest.of(0, 20));

    assertThat(page.getContent()).isNotEmpty();
    assertThat(Hibernate.isInitialized(page.getContent().get(0).getVenue())).isTrue();
  }

  private Concert createConcert() {
    Venue venue = venueRepository.saveAndFlush(
        Venue.create("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0));
    return Concert.create(
        "PF001", venue, "테스트 공연", Genre.POPULAR_MUSIC, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 2), "poster.jpg", Instant.now());
  }
}
