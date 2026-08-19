package com.fanroute.sync.domain.concert.batch;

import java.time.LocalDate;

import com.fanroute.sync.domain.concert.entity.Genre;

public record ConcertSyncDraft(
    String kopisConcertId,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    Genre genre,
    String posterUrl,
    String kopisVenueId,
    String venueName,
    String venueAddress,
    Double venueLatitude,
    Double venueLongitude) {

}
