package com.fanroute.sync.domain.concert.batch;

import java.time.Clock;
import java.time.Instant;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;

import lombok.RequiredArgsConstructor;

/** Spring Batch의 Chunk 트랜잭션 안에서 공연장과 공연을 Upsert합니다. */
@RequiredArgsConstructor
public class ConcertItemWriter implements ItemWriter<ConcertSyncDraft> {

  private final VenueRepository venueRepository;
  private final ConcertRepository concertRepository;
  private final Clock clock;

  @Override
  public void write(Chunk<? extends ConcertSyncDraft> chunk) {
    for (ConcertSyncDraft draft : chunk.getItems()) {
      Venue venue = upsertVenue(draft);
      upsertConcert(draft, venue);
    }
  }

  private Venue upsertVenue(ConcertSyncDraft draft) {
    return venueRepository.findByKopisVenueId(draft.kopisVenueId())
        .map(venue -> {
          venue.updateFromSync(
              draft.venueName(), draft.venueAddress(), draft.venueLatitude(),
              draft.venueLongitude());
          return venue;
        })
        .orElseGet(() -> venueRepository.save(Venue.create(
            draft.kopisVenueId(), draft.venueName(), draft.venueAddress(), draft.venueLatitude(),
            draft.venueLongitude())));
  }

  private void upsertConcert(ConcertSyncDraft draft, Venue venue) {
    Instant syncedAt = clock.instant();
    concertRepository.findByKopisConcertId(draft.kopisConcertId())
        .ifPresentOrElse(
            concert -> concert.updateFromSync(
                venue, draft.title(), draft.genre(), draft.startDate(), draft.endDate(),
                draft.posterUrl(), syncedAt),
            () -> concertRepository.save(Concert.create(
                draft.kopisConcertId(), venue, draft.title(), draft.genre(), draft.startDate(),
                draft.endDate(), draft.posterUrl(), syncedAt)));
  }
}
