package com.fanroute.sync.domain.concert.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import com.fanroute.sync.domain.concert.client.KopisClient;
import com.fanroute.sync.domain.concert.config.KopisProperties;
import com.fanroute.sync.domain.concert.dto.KopisDto;

@ExtendWith(MockitoExtension.class)
class ConcertItemReaderTest {

  @Mock
  private KopisClient kopisClient;

  private ConcertItemReader reader;

  @BeforeEach
  void setUp() {
    KopisProperties properties = new KopisProperties("test-key", "26", 10);
    Clock clock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneOffset.UTC);
    reader = new ConcertItemReader(kopisClient, properties, clock);
    reader.open(new ExecutionContext());
  }

  @Test
  @DisplayName("응답이 비어 있으면 31일 단위 구간을 모두 순회한 뒤 null을 반환한다")
  void returnsNullAfterExhaustingAllWindows() {
    when(kopisClient.getPerformances(
        anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(new KopisDto.PerformanceListResponse(List.of()));

    assertThat(reader.read()).isNull();

    verify(kopisClient, times(1)).getPerformances(
        eq("test-key"), eq("20260819"), eq("20260918"), eq(1), anyInt(), eq("26"));
    verify(kopisClient, times(1)).getPerformances(
        eq("test-key"), eq("20260919"), eq("20261019"), eq(1), anyInt(), eq("26"));
    verify(kopisClient, times(1)).getPerformances(
        eq("test-key"), eq("20261020"), eq("20261119"), eq(1), anyInt(), eq("26"));
    verify(kopisClient, times(3)).getPerformances(
        eq("test-key"), anyString(), anyString(), eq(1), anyInt(), eq("26"));
  }

  @Test
  @DisplayName("한 번에 최대 10건까지만 반환한다")
  void limitsToMaxSyncItems() {
    List<KopisDto.PerformanceSummary> summaries = IntStream.rangeClosed(1, 12)
        .mapToObj(index -> summary("PF%03d".formatted(index)))
        .toList();
    when(kopisClient.getPerformances(
        anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(new KopisDto.PerformanceListResponse(summaries));

    List<KopisDto.PerformanceSummary> read = readAll();

    assertThat(read).hasSize(10);
    verify(kopisClient, times(1)).getPerformances(
        anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString());
  }

  @Test
  @DisplayName("같은 공연 ID가 응답에 중복으로 들어와도 한 번만 반환한다")
  void deduplicatesByKopisConcertId() {
    when(kopisClient.getPerformances(
        anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(new KopisDto.PerformanceListResponse(
            List.of(summary("PF001"), summary("PF001"), summary("PF002"))));

    List<KopisDto.PerformanceSummary> read = readAll();

    assertThat(read).extracting(KopisDto.PerformanceSummary::kopisConcertId)
        .containsExactly("PF001", "PF002");
  }

  private List<KopisDto.PerformanceSummary> readAll() {
    return java.util.stream.Stream.generate(reader::read)
        .takeWhile(Objects::nonNull)
        .toList();
  }

  private KopisDto.PerformanceSummary summary(String kopisConcertId) {
    return new KopisDto.PerformanceSummary(
        kopisConcertId, "테스트 공연", "2026.09.01", "2026.09.02", "테스트홀", "poster.jpg", "대중음악");
  }
}
