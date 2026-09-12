package com.fanroute.sync.domain.concert.batch;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import com.fanroute.sync.domain.concert.batch.PerformanceTimeGuideParser.GeneratedSlot;
import com.fanroute.sync.domain.concert.batch.PerformanceTimeGuideParser.GenerationResult;
import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.entity.ScheduleParseStatus;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.domain.concert.repository.ConcertScheduleRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Spring Batch의 Chunk 트랜잭션 안에서 공연장과 공연을 Upsert합니다. */
@Slf4j
@RequiredArgsConstructor
public class ConcertItemWriter implements ItemWriter<ConcertSyncDraft> {

  private final VenueRepository venueRepository;
  private final ConcertRepository concertRepository;
  private final ConcertScheduleRepository concertScheduleRepository;
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
    Concert concert = concertRepository.findByKopisConcertId(draft.kopisConcertId())
        .map(existing -> {
          existing.updateFromSync(
              venue, draft.title(), draft.genre(), draft.startDate(), draft.endDate(),
              draft.posterUrl(), draft.performanceTimeGuide(), syncedAt);
          return existing;
        })
        .orElseGet(() -> concertRepository.save(Concert.create(
            draft.kopisConcertId(), venue, draft.title(), draft.genre(), draft.startDate(),
            draft.endDate(), draft.posterUrl(), draft.performanceTimeGuide(), syncedAt)));
    reconcileSchedules(concert);
  }

  /** 파싱이 성공(PARSED)한 경우에만 자동·미확정 회차를 새로 전개한 회차로 교체합니다. 관리자가 확정한
   * 회차는 건드리지 않고, 실패 시 기존 회차를 그대로 둔 채 파싱 상태만 기록합니다. */
  private void reconcileSchedules(Concert concert) {
    String newHash = PerformanceTimeGuideParser.hash(
        concert.getPerformanceTimeGuide(), concert.getStartDate(), concert.getEndDate());
    if (!concert.needsScheduleReparse(newHash, PerformanceTimeGuideParser.VERSION)) {
      return;
    }

    GenerationResult result = PerformanceTimeGuideParser.generate(
        concert.getPerformanceTimeGuide(), concert.getStartDate(), concert.getEndDate());
    concert.updateScheduleParseState(result.status(), PerformanceTimeGuideParser.VERSION, newHash);

    List<ConcertSchedule> active = concertScheduleRepository.findByConcertId(concert.getId());
    List<ConcertSchedule> autoUnconfirmed = active.stream().filter(ConcertSchedule::isProvisional)
        .toList();

    if (result.status() != ScheduleParseStatus.PARSED) {
      Instant now = clock.instant();
      autoUnconfirmed.forEach(schedule -> schedule.delete(now));
      return;
    }

    List<ConcertSchedule> confirmed = active.stream().filter(s -> !s.isProvisional()).toList();

    Set<GeneratedSlot> occupiedByConfirmed = confirmed.stream()
        .map(s -> new GeneratedSlot(s.getPerformanceDate(), s.getPerformanceTime()))
        .collect(Collectors.toSet());

    List<GeneratedSlot> accepted = new ArrayList<>();
    for (GeneratedSlot slot : result.slots()) {
      if (occupiedByConfirmed.contains(slot)) {
        log.warn("공연 {}: 관리자가 확정한 회차와 겹쳐 자동 생성을 건너뜀 ({} {})",
            concert.getKopisConcertId(), slot.date(), slot.time());
        continue;
      }
      accepted.add(slot);
    }

    Instant now = clock.instant();
    autoUnconfirmed.forEach(schedule -> schedule.delete(now));
    // soft-delete를 신규 INSERT보다 먼저 flush — 부분 유니크 인덱스 충돌 방지.
    concertScheduleRepository.flush();

    Map<LocalDate, List<ConcertSchedule>> confirmedByDate = confirmed.stream()
        .collect(Collectors.groupingBy(ConcertSchedule::getPerformanceDate));
    Map<LocalDate, List<GeneratedSlot>> acceptedByDate = accepted.stream()
        .collect(Collectors.groupingBy(GeneratedSlot::date));

    for (Map.Entry<LocalDate, List<GeneratedSlot>> entry : acceptedByDate.entrySet()) {
      LocalDate date = entry.getKey();
      List<ConcertSchedule> newSchedules = entry.getValue().stream()
          .map(slot -> ConcertSchedule.createFromParsedSlot(concert, 0, slot.date(), slot.time()))
          .toList();
      concertScheduleRepository.saveAll(newSchedules);

      List<ConcertSchedule> sameDate = new ArrayList<>(newSchedules);
      sameDate.addAll(confirmedByDate.getOrDefault(date, List.of()));
      ConcertSchedule.renumberByTime(sameDate);
    }
  }
}
