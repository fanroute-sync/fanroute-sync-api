package com.fanroute.sync.domain.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 8, 19);

  @Mock
  private ConcertRepository concertRepository;

  private ConcertService concertService;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    concertService = new ConcertService(concertRepository, clock);
  }

  @Test
  @DisplayName("장르 없이 조회하면 종료되지 않은 공연 전체를 조회한다")
  void getsAllUpcomingConcertsWithoutGenre() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Concert> page = new PageImpl<>(List.of());
    when(concertRepository.findByEndDateGreaterThanEqual(TODAY, pageable)).thenReturn(page);

    Page<Concert> result = concertService.getConcerts(null, pageable);

    assertThat(result).isSameAs(page);
    verify(concertRepository, never())
        .findByEndDateGreaterThanEqualAndGenreName(any(), any(), any());
  }

  @Test
  @DisplayName("장르가 주어지면 해당 장르만 필터링해서 조회한다")
  void getsConcertsFilteredByGenre() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Concert> page = new PageImpl<>(List.of());
    when(concertRepository.findByEndDateGreaterThanEqualAndGenreName(
        TODAY, Genre.POPULAR_MUSIC, pageable))
        .thenReturn(page);

    Page<Concert> result = concertService.getConcerts(Genre.POPULAR_MUSIC, pageable);

    assertThat(result).isSameAs(page);
    verify(concertRepository, never()).findByEndDateGreaterThanEqual(any(), any());
  }

  @Test
  @DisplayName("존재하는 공연을 ID로 조회한다")
  void getsConcertById() {
    Venue venue = Venue.create("FC001", "테스트홀", "부산", 35.1, 129.0);
    Concert concert = Concert.create(
        "PF001", venue, "테스트 공연", Genre.POPULAR_MUSIC, TODAY, TODAY.plusDays(1), "poster.jpg",
        Instant.now());
    when(concertRepository.findWithVenueById(1L)).thenReturn(Optional.of(concert));

    assertThat(concertService.getConcert(1L)).isSameAs(concert);
  }

  @Test
  @DisplayName("존재하지 않는 공연을 조회하면 예외가 발생한다")
  void throwsWhenConcertNotFound() {
    when(concertRepository.findWithVenueById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> concertService.getConcert(1L))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.CONCERT_NOT_FOUND));
  }
}
