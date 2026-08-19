package com.fanroute.sync.domain.concert.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class ConcertItemWriterTest {

  @Mock
  private VenueRepository venueRepository;
  @Mock
  private ConcertRepository concertRepository;

  private ConcertItemWriter writer;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneOffset.UTC);
    writer = new ConcertItemWriter(venueRepository, concertRepository, clock);
  }

  @Test
  @DisplayName("신규 공연장·공연은 새로 저장한다")
  void savesNewVenueAndConcert() {
    when(venueRepository.findByKopisVenueId("FC001")).thenReturn(Optional.empty());
    when(venueRepository.save(any(Venue.class)))
        .thenReturn(Venue.create("FC001", "테스트홀", "부산", 35.1, 129.0));
    when(concertRepository.findByKopisConcertId("PF001")).thenReturn(Optional.empty());

    writer.write(new Chunk<>(draft()));

    verify(venueRepository).save(any(Venue.class));
    ArgumentCaptor<Concert> captor = ArgumentCaptor.forClass(Concert.class);
    verify(concertRepository).save(captor.capture());
    assertThat(captor.getValue().getKopisConcertId()).isEqualTo("PF001");
    assertThat(captor.getValue().getGenreName()).isEqualTo(Genre.POPULAR_MUSIC);
  }

  @Test
  @DisplayName("이미 저장된 공연장·공연이면 새로 저장하지 않고 갱신한다")
  void updatesExistingVenueAndConcert() {
    Venue existingVenue = Venue.create("FC001", "옛 이름", "옛 주소", 0.0, 0.0);
    Concert existingConcert = Concert.create(
        "PF001", existingVenue, "옛 공연명", Genre.PLAY, LocalDate.of(2020, 1, 1),
        LocalDate.of(2020, 1, 2), null, Instant.parse("2020-01-01T00:00:00Z"));
    when(venueRepository.findByKopisVenueId("FC001")).thenReturn(Optional.of(existingVenue));
    when(concertRepository.findByKopisConcertId("PF001"))
        .thenReturn(Optional.of(existingConcert));

    writer.write(new Chunk<>(draft()));

    verify(venueRepository, never()).save(any());
    verify(concertRepository, never()).save(any());
    assertThat(existingVenue.getName()).isEqualTo("테스트홀");
    assertThat(existingConcert.getTitle()).isEqualTo("테스트 공연");
  }

  private ConcertSyncDraft draft() {
    return new ConcertSyncDraft(
        "PF001", "테스트 공연", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2),
        Genre.POPULAR_MUSIC, "poster.jpg", "FC001", "테스트홀", "부산", 35.1, 129.0);
  }
}
