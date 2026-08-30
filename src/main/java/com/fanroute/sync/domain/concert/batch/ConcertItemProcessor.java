package com.fanroute.sync.domain.concert.batch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.fanroute.sync.domain.concert.client.KopisClient;
import com.fanroute.sync.domain.concert.config.KopisProperties;
import com.fanroute.sync.domain.concert.dto.KopisDto;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * 잘못된 KOPIS 응답을 Skip 대상으로 변환하고 전송 오류는 재시도 정책에 전달합니다.
 */
@RequiredArgsConstructor
public class ConcertItemProcessor
    implements ItemProcessor<KopisDto.PerformanceSummary, ConcertSyncDraft> {

  private static final DateTimeFormatter KOPIS_RESPONSE_DATE_FORMAT = DateTimeFormatter.ofPattern(
      "yyyy.MM.dd");

  private final KopisClient kopisClient;
  private final KopisProperties kopisProperties;

  @Override
  public ConcertSyncDraft process(KopisDto.PerformanceSummary summary) {
    KopisDto.PerformanceDetail detail = fetchDetail(summary.kopisConcertId());
    if (isBlank(detail.kopisConcertId()) || isBlank(detail.title())) {
      throw new BusinessException(ConcertErrorCode.KOPIS_RESPONSE_INVALID);
    }
    LocalDate startDate = parseDate(detail.startDate());
    LocalDate endDate = parseDate(detail.endDate());
    Genre genre = parseGenre(detail.genreName());

    String kopisVenueId = detail.kopisVenueId();
    if (kopisVenueId == null || kopisVenueId.isBlank()) {
      throw new BusinessException(ConcertErrorCode.KOPIS_RESPONSE_INVALID);
    }
    KopisDto.VenueDetail venueDetail = fetchVenueDetail(kopisVenueId);
    String venueName = venueDetail.name() != null ? venueDetail.name() : detail.venueName();

    return new ConcertSyncDraft(
        detail.kopisConcertId(), detail.title(), startDate, endDate, genre, detail.posterUrl(),
        kopisVenueId, venueName, venueDetail.address(), venueDetail.latitude(),
        venueDetail.longitude());
  }

  private KopisDto.PerformanceDetail fetchDetail(String kopisConcertId) {
    KopisDto.PerformanceDetailResponse response =
        kopisClient.getPerformanceDetail(kopisConcertId, kopisProperties.serviceKey());
    if (response == null || response.performance() == null) {
      throw new BusinessException(ConcertErrorCode.KOPIS_RESPONSE_INVALID);
    }
    return response.performance();
  }

  private KopisDto.VenueDetail fetchVenueDetail(String kopisVenueId) {
    KopisDto.VenueDetailResponse response =
        kopisClient.getVenueDetail(kopisVenueId, kopisProperties.serviceKey());
    if (response == null || response.venue() == null) {
      throw new BusinessException(ConcertErrorCode.KOPIS_RESPONSE_INVALID);
    }
    return response.venue();
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

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}