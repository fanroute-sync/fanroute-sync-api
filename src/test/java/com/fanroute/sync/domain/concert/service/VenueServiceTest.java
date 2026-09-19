package com.fanroute.sync.domain.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.domain.concert.repository.VenuePlaceCollectionRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

  @Mock
  private VenueRepository venueRepository;

  @Mock
  private ConcertRepository concertRepository;

  @Mock
  private VenuePlaceCollectionRepository collectionRepository;

  private VenueService service;

  @BeforeEach
  void setUp() {
    service = new VenueService(venueRepository, concertRepository, collectionRepository,
        Clock.fixed(Instant.parse("2026-09-19T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  @DisplayName("공연장 상세 조회 시 추천 데이터 개수를 함께 반환한다")
  void countsVenueRelatedData() {
    Venue venue = Venue.create("FC000001", "부산 공연장", "부산시", 35.1, 129.1);
    when(venueRepository.findById(1L)).thenReturn(Optional.of(venue));
    when(concertRepository.countByVenueIdAndEndDateGreaterThanEqual(
        1L, java.time.LocalDate.of(2026, 9, 19))).thenReturn(3L);
    when(collectionRepository.countByVenueId(1L)).thenReturn(2L);

    Venue venueResult = service.getVenue(1L);

    assertThat(venueResult.getName()).isEqualTo("부산 공연장");
    assertThat(service.countUpcomingConcerts(1L)).isEqualTo(3L);
    assertThat(service.countPlaceCollections(1L)).isEqualTo(2L);
  }

  @Test
  @DisplayName("존재하지 않는 공연장 상세 조회 시 예외가 발생한다")
  void throwsWhenVenueMissing() {
    when(venueRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getVenue(1L))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ConcertErrorCode.VENUE_NOT_FOUND);
  }
}
