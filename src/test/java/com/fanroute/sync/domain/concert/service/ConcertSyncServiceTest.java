package com.fanroute.sync.domain.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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
import com.fanroute.sync.global.external.ExternalApiErrorType;
import com.fanroute.sync.global.external.ExternalApiException;

@ExtendWith(MockitoExtension.class)
class ConcertSyncServiceTest {

  private static final String SERVICE_KEY = "test-key";

  @Mock
  private KopisClient kopisClient;
  @Mock
  private VenueRepository venueRepository;
  @Mock
  private ConcertRepository concertRepository;

  private ConcertSyncService service;

  @BeforeEach
  void setUp() {
    KopisProperties properties = new KopisProperties(SERVICE_KEY, "26");
    Clock clock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneOffset.UTC);
    service = new ConcertSyncService(
        kopisClient, properties, venueRepository, concertRepository, clock);
  }

  @Test
  @DisplayName("신규 공연과 공연장을 동기화하면 새로 저장한다")
  void syncsNewPerformanceAndVenue() {
    when(kopisClient.getPerformanceDetail("PF001", SERVICE_KEY))
        .thenReturn(new KopisDto.PerformanceDetailResponse(performanceDetail()));
    when(kopisClient.getVenueDetail("FC001", SERVICE_KEY))
        .thenReturn(new KopisDto.VenueDetailResponse(venueDetail()));
    when(venueRepository.findByKopisVenueId("FC001")).thenReturn(Optional.empty());
    when(venueRepository.save(any(Venue.class)))
        .thenReturn(Venue.create("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0));
    when(concertRepository.findByKopisConcertId("PF001")).thenReturn(Optional.empty());

    service.syncPerformance("PF001");

    verify(venueRepository).save(any(Venue.class));
    ArgumentCaptor<Concert> captor = ArgumentCaptor.forClass(Concert.class);
    verify(concertRepository).save(captor.capture());
    Concert saved = captor.getValue();
    assertThat(saved.getKopisConcertId()).isEqualTo("PF001");
    assertThat(saved.getTitle()).isEqualTo("테스트 공연");
    assertThat(saved.getGenreName()).isEqualTo(Genre.POPULAR_MUSIC);
    assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 2));
  }

  @Test
  @DisplayName("이미 저장된 공연과 공연장이면 새로 저장하지 않고 갱신한다")
  void updatesExistingPerformanceAndVenue() {
    Venue existingVenue = Venue.create("FC001", "옛 이름", "옛 주소", 0.0, 0.0);
    Concert existingConcert = Concert.create(
        "PF001", existingVenue, "옛 공연명", Genre.PLAY, LocalDate.of(2020, 1, 1),
        LocalDate.of(2020, 1, 2), null, Instant.parse("2020-01-01T00:00:00Z"));
    when(kopisClient.getPerformanceDetail("PF001", SERVICE_KEY))
        .thenReturn(new KopisDto.PerformanceDetailResponse(performanceDetail()));
    when(kopisClient.getVenueDetail("FC001", SERVICE_KEY))
        .thenReturn(new KopisDto.VenueDetailResponse(venueDetail()));
    when(venueRepository.findByKopisVenueId("FC001")).thenReturn(Optional.of(existingVenue));
    when(concertRepository.findByKopisConcertId("PF001"))
        .thenReturn(Optional.of(existingConcert));

    service.syncPerformance("PF001");

    verify(venueRepository, org.mockito.Mockito.never()).save(any());
    verify(concertRepository, org.mockito.Mockito.never()).save(any());
    assertThat(existingVenue.getName()).isEqualTo("테스트홀");
    assertThat(existingConcert.getTitle()).isEqualTo("테스트 공연");
    assertThat(existingConcert.getVenue()).isSameAs(existingVenue);
  }

  @Test
  @DisplayName("KOPIS 응답에 공연장 ID가 없으면 응답 오류로 변환한다")
  void rejectsMissingVenueId() {
    KopisDto.PerformanceDetail detailWithoutVenue = new KopisDto.PerformanceDetail(
        "PF001", null, "테스트 공연", "2026.09.01", "2026.09.02", "테스트홀", "poster.jpg", "대중음악",
        "공연중");
    when(kopisClient.getPerformanceDetail("PF001", SERVICE_KEY))
        .thenReturn(new KopisDto.PerformanceDetailResponse(detailWithoutVenue));

    assertThatThrownBy(() -> service.syncPerformance("PF001"))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.KOPIS_RESPONSE_INVALID));
  }

  @Test
  @DisplayName("KOPIS 응답의 장르명이 알 수 없는 값이면 응답 오류로 변환한다")
  void rejectsUnknownGenre() {
    KopisDto.PerformanceDetail detailWithUnknownGenre = new KopisDto.PerformanceDetail(
        "PF001", "FC001", "테스트 공연", "2026.09.01", "2026.09.02", "테스트홀", "poster.jpg", "알수없는장르",
        "공연중");
    when(kopisClient.getPerformanceDetail("PF001", SERVICE_KEY))
        .thenReturn(new KopisDto.PerformanceDetailResponse(detailWithUnknownGenre));

    assertThatThrownBy(() -> service.syncPerformance("PF001"))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.KOPIS_RESPONSE_INVALID));
    verifyNoInteractions(venueRepository, concertRepository);
  }

  @Test
  @DisplayName("KOPIS 응답의 날짜 형식이 잘못되면 DB 변경 전에 응답 오류로 변환한다")
  void rejectsInvalidDateBeforePersistence() {
    KopisDto.PerformanceDetail detailWithInvalidDate = new KopisDto.PerformanceDetail(
        "PF001", "FC001", "테스트 공연", "2026-09-01", "2026.09.02", "테스트홀",
        "poster.jpg", "대중음악", "공연중");
    when(kopisClient.getPerformanceDetail("PF001", SERVICE_KEY))
        .thenReturn(new KopisDto.PerformanceDetailResponse(detailWithInvalidDate));

    assertThatThrownBy(() -> service.syncPerformance("PF001"))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.KOPIS_RESPONSE_INVALID));
    verifyNoInteractions(venueRepository, concertRepository);
  }

  @Test
  @DisplayName("KOPIS 서버 장애는 공통 예외로 변환한다")
  void translatesKopisFailureToBusinessException() {
    when(kopisClient.getPerformanceDetail("PF001", SERVICE_KEY)).thenThrow(
        ExternalApiException.responseError(
            ExternalApiErrorType.SERVER_ERROR, HttpStatus.BAD_GATEWAY, "server error"));

    assertThatThrownBy(() -> service.syncPerformance("PF001"))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.KOPIS_API_UNAVAILABLE));
  }

  @Test
  @DisplayName("일부 공연 동기화가 실패해도 나머지는 계속 진행한다")
  void continuesSyncingAfterPartialFailure() {
    KopisDto.PerformanceSummary summary1 = new KopisDto.PerformanceSummary(
        "PF001", "성공 공연", "2026.09.01", "2026.09.02", "테스트홀", "poster.jpg", "대중음악");
    KopisDto.PerformanceSummary summary2 = new KopisDto.PerformanceSummary(
        "PF002", "실패 공연", "2026.09.01", "2026.09.02", "테스트홀", "poster.jpg", "대중음악");
    when(kopisClient.getPerformances(
        anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(new KopisDto.PerformanceListResponse(List.of(summary1, summary2)));

    when(kopisClient.getPerformanceDetail("PF001", SERVICE_KEY))
        .thenReturn(new KopisDto.PerformanceDetailResponse(performanceDetail()));
    when(kopisClient.getVenueDetail("FC001", SERVICE_KEY))
        .thenReturn(new KopisDto.VenueDetailResponse(venueDetail()));
    when(venueRepository.findByKopisVenueId("FC001")).thenReturn(Optional.empty());
    when(venueRepository.save(any(Venue.class)))
        .thenReturn(Venue.create("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0));
    when(concertRepository.findByKopisConcertId("PF001")).thenReturn(Optional.empty());

    when(kopisClient.getPerformanceDetail("PF002", SERVICE_KEY)).thenThrow(
        ExternalApiException.responseError(
            ExternalApiErrorType.SERVER_ERROR, HttpStatus.BAD_GATEWAY, "server error"));

    ConcertSyncService.SyncResult result = service.syncConcerts();

    assertThat(result.total()).isEqualTo(2);
    assertThat(result.succeeded()).isEqualTo(1);
    assertThat(result.failed()).isEqualTo(1);
  }

  @Test
  @DisplayName("동기화 기간이 31일을 넘으면 KOPIS 목록조회를 31일 이하 구간으로 나눠 호출한다")
  void splitsSyncPeriodIntoWindowsWithinKopisLimit() {
    when(kopisClient.getPerformances(
        anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(new KopisDto.PerformanceListResponse(List.of()));

    service.syncConcerts();

    ArgumentCaptor<String> startDateCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> endDateCaptor = ArgumentCaptor.forClass(String.class);
    verify(kopisClient, times(3)).getPerformances(
        eq(SERVICE_KEY), startDateCaptor.capture(), endDateCaptor.capture(), eq(1), eq(10),
        eq("26"));

    assertThat(startDateCaptor.getAllValues())
        .containsExactly("20260819", "20260919", "20261020");
    assertThat(endDateCaptor.getAllValues())
        .containsExactly("20260918", "20261019", "20261119");
  }

  @Test
  @DisplayName("한 번의 동기화에서 공연을 최대 10개까지만 처리한다")
  void limitsSyncToTenPerformances() {
    List<KopisDto.PerformanceSummary> summaries = java.util.stream.IntStream.rangeClosed(1, 12)
        .mapToObj(index -> new KopisDto.PerformanceSummary(
            "PF%03d".formatted(index), "공연 " + index, "2026.09.01", "2026.09.02",
            "테스트홀", "poster.jpg", "대중음악"))
        .toList();
    when(kopisClient.getPerformances(
        anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(new KopisDto.PerformanceListResponse(summaries));
    when(kopisClient.getPerformanceDetail(anyString(), eq(SERVICE_KEY))).thenThrow(
        ExternalApiException.responseError(
            ExternalApiErrorType.SERVER_ERROR, HttpStatus.BAD_GATEWAY, "server error"));

    ConcertSyncService.SyncResult result = service.syncConcerts();

    assertThat(result.total()).isEqualTo(10);
    assertThat(result.failed()).isEqualTo(10);
    verify(kopisClient, times(10)).getPerformanceDetail(anyString(), eq(SERVICE_KEY));
    verify(kopisClient).getPerformances(
        eq(SERVICE_KEY), eq("20260819"), eq("20260918"), eq(1), eq(10), eq("26"));
  }

  private KopisDto.PerformanceDetail performanceDetail() {
    return new KopisDto.PerformanceDetail(
        "PF001", "FC001", "테스트 공연", "2026.09.01", "2026.09.02", "테스트홀", "poster.jpg",
        "대중음악", "공연중");
  }

  private KopisDto.VenueDetail venueDetail() {
    return new KopisDto.VenueDetail("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0);
  }
}
