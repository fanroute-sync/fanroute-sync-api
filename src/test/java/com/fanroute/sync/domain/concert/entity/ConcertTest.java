package com.fanroute.sync.domain.concert.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConcertTest {

  private final Venue venue = Venue.create("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0);

  @Test
  @DisplayName("공연을 생성하면 입력한 값이 그대로 저장된다")
  void createsConcertWithGivenValues() {
    Instant syncedAt = Instant.parse("2026-08-19T00:00:00Z");

    Concert concert = Concert.create(
        "PF001", venue, "테스트 공연", Genre.POPULAR_MUSIC,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), "poster.jpg", syncedAt);

    assertThat(concert.getKopisConcertId()).isEqualTo("PF001");
    assertThat(concert.getVenue()).isSameAs(venue);
    assertThat(concert.getTitle()).isEqualTo("테스트 공연");
    assertThat(concert.getGenreName()).isEqualTo(Genre.POPULAR_MUSIC);
    assertThat(concert.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(concert.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 2));
    assertThat(concert.getPosterUrl()).isEqualTo("poster.jpg");
    assertThat(concert.getLastSyncedAt()).isEqualTo(syncedAt);
  }

  @Test
  @DisplayName("재동기화하면 공연 정보가 최신값으로 갱신된다")
  void updatesFromSync() {
    Instant firstSync = Instant.parse("2026-08-19T00:00:00Z");
    Instant secondSync = Instant.parse("2026-08-20T00:00:00Z");
    Concert concert = Concert.create(
        "PF001", venue, "테스트 공연", Genre.POPULAR_MUSIC,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), "poster.jpg", firstSync);
    Venue newVenue = Venue.create("FC002", "다른 홀", "부산 사상구", 35.2, 129.1);

    concert.updateFromSync(
        newVenue, "변경된 공연명", Genre.DANCE, LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 6),
        "new-poster.jpg", secondSync);

    assertThat(concert.getVenue()).isSameAs(newVenue);
    assertThat(concert.getTitle()).isEqualTo("변경된 공연명");
    assertThat(concert.getGenreName()).isEqualTo(Genre.DANCE);
    assertThat(concert.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 5));
    assertThat(concert.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 6));
    assertThat(concert.getPosterUrl()).isEqualTo("new-poster.jpg");
    assertThat(concert.getLastSyncedAt()).isEqualTo(secondSync);
  }
}
