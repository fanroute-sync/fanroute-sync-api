package com.fanroute.sync.domain.concert.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fanroute.sync.domain.concert.client.KopisClient;
import com.fanroute.sync.domain.concert.config.KopisProperties;
import com.fanroute.sync.domain.concert.dto.KopisDto;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.external.ExternalApiErrorType;
import com.fanroute.sync.global.external.ExternalApiException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ConcertItemProcessorTest {

  private static final String SERVICE_KEY = "test-key";

  @Mock
  private KopisClient kopisClient;

  private ConcertItemProcessor processor;

  @BeforeEach
  void setUp() {
    KopisProperties properties = new KopisProperties(SERVICE_KEY, "26", 0);
    processor = new ConcertItemProcessor(kopisClient, properties);
  }

  @Test
  @DisplayName("상세·공연장 조회 결과를 검증·보강해 Draft로 변환한다")
  void buildsDraftFromDetailAndVenue() {
    when(kopisClient.getPerformanceDetail("PF001", SERVICE_KEY))
        .thenReturn(new KopisDto.PerformanceDetailResponse(performanceDetail()));
    when(kopisClient.getVenueDetail("FC001", SERVICE_KEY))
        .thenReturn(new KopisDto.VenueDetailResponse(venueDetail()));

    ConcertSyncDraft draft = processor.process(summary());

    assertThat(draft.kopisConcertId()).isEqualTo("PF001");
    assertThat(draft.title()).isEqualTo("테스트 공연");
    assertThat(draft.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(draft.endDate()).isEqualTo(LocalDate.of(2026, 9, 2));
    assertThat(draft.genre()).isEqualTo(Genre.POPULAR_MUSIC);
    assertThat(draft.performanceTimeGuide())
        .isEqualTo("화요일(20:00), 토요일(16:00,19:00)");
    assertThat(draft.kopisVenueId()).isEqualTo("FC001");
    assertThat(draft.venueName()).isEqualTo("테스트홀");
  }

  @Test
  @DisplayName("공연장 ID가 없으면 응답 오류로 처리한다")
  void rejectsMissingVenueId() {
    KopisDto.PerformanceDetail detailWithoutVenue = new KopisDto.PerformanceDetail(
        "PF001", null, "테스트 공연", "2026.09.01", "2026.09.02", "테스트홀", "poster.jpg", "대중음악",
        "공연중");
    when(kopisClient.getPerformanceDetail("PF001", SERVICE_KEY))
        .thenReturn(new KopisDto.PerformanceDetailResponse(detailWithoutVenue));

    assertThatThrownBy(() -> processor.process(summary()))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.KOPIS_RESPONSE_INVALID));
  }

  @Test
  @DisplayName("공연 ID 또는 제목이 비어 있으면 응답 오류로 처리한다")
  void rejectsBlankConcertIdOrTitle() {
    KopisDto.PerformanceDetail blankTitle = new KopisDto.PerformanceDetail(
        "PF001", "FC001", " ", "2026.09.01", "2026.09.02", "테스트홀", "poster.jpg", "대중음악",
        "공연중");
    when(kopisClient.getPerformanceDetail("PF001", SERVICE_KEY))
        .thenReturn(new KopisDto.PerformanceDetailResponse(blankTitle));

    assertThatThrownBy(() -> processor.process(summary()))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.KOPIS_RESPONSE_INVALID));
  }

  @Test
  @DisplayName("날짜 형식이 잘못되면 응답 오류로 처리한다")
  void rejectsInvalidDate() {
    KopisDto.PerformanceDetail invalidDate = new KopisDto.PerformanceDetail(
        "PF001", "FC001", "테스트 공연", "2026-09-01", "2026.09.02", "테스트홀", "poster.jpg",
        "대중음악", "공연중");
    when(kopisClient.getPerformanceDetail("PF001", SERVICE_KEY))
        .thenReturn(new KopisDto.PerformanceDetailResponse(invalidDate));

    assertThatThrownBy(() -> processor.process(summary()))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ConcertErrorCode.KOPIS_RESPONSE_INVALID));
  }

  @Test
  @DisplayName("전송 오류는 감싸지 않고 그대로 던져 Step의 재시도 정책이 처리하게 한다")
  void propagatesTransportErrorRaw() {
    when(kopisClient.getPerformanceDetail("PF001", SERVICE_KEY)).thenThrow(
        ExternalApiException.responseError(
            ExternalApiErrorType.SERVER_ERROR, HttpStatus.BAD_GATEWAY, "server error"));

    assertThatThrownBy(() -> processor.process(summary()))
        .isInstanceOf(ExternalApiException.class);
  }

  private KopisDto.PerformanceSummary summary() {
    return new KopisDto.PerformanceSummary(
        "PF001", "테스트 공연", "2026.09.01", "2026.09.02", "테스트홀", "poster.jpg", "대중음악");
  }

  private KopisDto.PerformanceDetail performanceDetail() {
    return new KopisDto.PerformanceDetail(
        "PF001", "FC001", "테스트 공연", "2026.09.01", "2026.09.02", "테스트홀", "poster.jpg",
        "대중음악", "공연중", "화요일(20:00), 토요일(16:00,19:00)");
  }

  private KopisDto.VenueDetail venueDetail() {
    return new KopisDto.VenueDetail("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0);
  }
}
