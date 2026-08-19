package com.fanroute.sync.domain.concert.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.concert.client.KopisClient;
import com.fanroute.sync.domain.concert.config.KopisProperties;
import com.fanroute.sync.domain.concert.dto.KopisDto;
import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.external.ExternalApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcertSyncService {

  private static final DateTimeFormatter KOPIS_REQUEST_DATE_FORMAT = DateTimeFormatter.ofPattern(
      "yyyyMMdd");
  private static final DateTimeFormatter KOPIS_RESPONSE_DATE_FORMAT = DateTimeFormatter.ofPattern(
      "yyyy.MM.dd");
  private static final int SYNC_PERIOD_MONTHS = 3;
  private static final int MAX_SYNC_ITEMS = 10;
  private static final int MAX_DATE_RANGE_DAYS = 31; // KOPIS 목록조회 stdate~eddate 최대 허용 범위

  private final KopisClient kopisClient;
  private final KopisProperties kopisProperties;
  private final VenueRepository venueRepository;
  private final ConcertRepository concertRepository;
  private final Clock clock;

  @Scheduled(cron = "${kopis.sync-cron:0 0 4 * * *}")
  public void syncConcertsOnSchedule() {
    SyncResult result = syncConcerts();
    log.info("KOPIS 공연 정기 동기화 완료: total={}, succeeded={}, failed={}",
        result.total(), result.succeeded(), result.failed());
  }

  public SyncResult syncConcerts() {
    LocalDate today = LocalDate.now(clock);
    LocalDate periodEnd = today.plusMonths(SYNC_PERIOD_MONTHS);

    List<KopisDto.PerformanceSummary> summaries = fetchAllPerformances(today, periodEnd);

    int succeeded = 0;
    int failed = 0;
    for (KopisDto.PerformanceSummary summary : summaries) {
      try {
        syncPerformance(summary.kopisConcertId());
        succeeded++;
      } catch (Exception exception) {
        failed++;
        log.warn("공연 동기화 실패: kopisConcertId={}, message={}",
            summary.kopisConcertId(), exception.getMessage());
      }
    }
    return new SyncResult(summaries.size(), succeeded, failed);
  }

  /**
   * KOPIS 목록조회는 {@code stdate}~{@code eddate}가 최대 {@value MAX_DATE_RANGE_DAYS}일이라,
   * 전체 동기화 기간을 그 이하 단위 구간으로 나눠 여러 번 조회합니다. 공연 기간이 구간 경계에
   * 걸치면 같은 공연이 여러 구간에서 조회될 수 있어 공연 ID 기준으로 중복을 제거합니다.
   */
  private List<KopisDto.PerformanceSummary> fetchAllPerformances(LocalDate from, LocalDate to) {
    Map<String, KopisDto.PerformanceSummary> uniqueByConcertId = new LinkedHashMap<>();
    LocalDate windowStart = from;
    while (!windowStart.isAfter(to)) {
      LocalDate maxWindowEnd = windowStart.plusDays(MAX_DATE_RANGE_DAYS - 1);
      LocalDate windowEnd = maxWindowEnd.isAfter(to) ? to : maxWindowEnd;
      int remaining = MAX_SYNC_ITEMS - uniqueByConcertId.size();
      for (KopisDto.PerformanceSummary summary : fetchPerformancesInWindow(
          windowStart, windowEnd, remaining)) {
        uniqueByConcertId.putIfAbsent(summary.kopisConcertId(), summary);
        if (uniqueByConcertId.size() == MAX_SYNC_ITEMS) {
          return new ArrayList<>(uniqueByConcertId.values());
        }
      }
      windowStart = windowEnd.plusDays(1);
    }
    return new ArrayList<>(uniqueByConcertId.values());
  }

  private List<KopisDto.PerformanceSummary> fetchPerformancesInWindow(
      LocalDate from, LocalDate to, int rows) {
    String startDate = from.format(KOPIS_REQUEST_DATE_FORMAT);
    String endDate = to.format(KOPIS_REQUEST_DATE_FORMAT);
    KopisDto.PerformanceListResponse response;
    try {
      response = kopisClient.getPerformances(
          kopisProperties.serviceKey(), startDate, endDate, 1, rows,
          kopisProperties.regionCode());
    } catch (ExternalApiException exception) {
      throw new BusinessException(ConcertErrorCode.KOPIS_API_UNAVAILABLE);
    }
    return response.performancesOrEmpty();
  }

  @Transactional
  public void syncPerformance(String kopisConcertId) {
    KopisDto.PerformanceDetail detail = fetchPerformanceDetail(kopisConcertId);
    LocalDate startDate = parseDate(detail.startDate());
    LocalDate endDate = parseDate(detail.endDate());
    Genre genre = parseGenre(detail.genreName());
    Venue venue = syncVenue(detail.kopisVenueId(), detail.venueName());
    upsertConcert(detail, venue, startDate, endDate, genre);
  }

  private KopisDto.PerformanceDetail fetchPerformanceDetail(String kopisConcertId) {
    KopisDto.PerformanceDetailResponse response;
    try {
      response = kopisClient.getPerformanceDetail(kopisConcertId, kopisProperties.serviceKey());
    } catch (ExternalApiException exception) {
      throw new BusinessException(ConcertErrorCode.KOPIS_API_UNAVAILABLE);
    }
    if (response == null || response.performance() == null) {
      throw new BusinessException(ConcertErrorCode.KOPIS_RESPONSE_INVALID);
    }
    return response.performance();
  }

  private Venue syncVenue(String kopisVenueId, String fallbackName) {
    if (kopisVenueId == null || kopisVenueId.isBlank()) {
      throw new BusinessException(ConcertErrorCode.KOPIS_RESPONSE_INVALID);
    }
    KopisDto.VenueDetail venueDetail = fetchVenueDetail(kopisVenueId);
    String name = venueDetail.name() != null ? venueDetail.name() : fallbackName;

    return venueRepository.findByKopisVenueId(kopisVenueId)
        .map(venue -> {
          venue.updateFromSync(
              name, venueDetail.address(), venueDetail.latitude(), venueDetail.longitude());
          return venue;
        })
        .orElseGet(() -> venueRepository.save(Venue.create(
            kopisVenueId, name, venueDetail.address(), venueDetail.latitude(),
            venueDetail.longitude())));
  }

  private KopisDto.VenueDetail fetchVenueDetail(String kopisVenueId) {
    KopisDto.VenueDetailResponse response;
    try {
      response = kopisClient.getVenueDetail(kopisVenueId, kopisProperties.serviceKey());
    } catch (ExternalApiException exception) {
      throw new BusinessException(ConcertErrorCode.KOPIS_API_UNAVAILABLE);
    }
    if (response == null || response.venue() == null) {
      throw new BusinessException(ConcertErrorCode.KOPIS_RESPONSE_INVALID);
    }
    return response.venue();
  }

  private void upsertConcert(KopisDto.PerformanceDetail detail, Venue venue,
      LocalDate startDate, LocalDate endDate, Genre genre) {
    Instant syncedAt = clock.instant();

    concertRepository.findByKopisConcertId(detail.kopisConcertId())
        .ifPresentOrElse(
            concert -> concert.updateFromSync(
                venue, detail.title(), genre, startDate, endDate, detail.posterUrl(), syncedAt),
            () -> concertRepository.save(Concert.create(
                detail.kopisConcertId(), venue, detail.title(), genre, startDate, endDate,
                detail.posterUrl(), syncedAt)));
  }

  private LocalDate parseDate(String kopisDate) {
    try {
      return LocalDate.parse(kopisDate, KOPIS_RESPONSE_DATE_FORMAT);
    } catch (DateTimeParseException | NullPointerException exception) {
      throw new BusinessException(ConcertErrorCode.KOPIS_RESPONSE_INVALID);
    }
  }

  private Genre parseGenre(String kopisGenreName) {
    try {
      return Genre.fromLabel(kopisGenreName);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(ConcertErrorCode.KOPIS_RESPONSE_INVALID);
    }
  }

  public record SyncResult(int total, int succeeded, int failed) {

  }
}
