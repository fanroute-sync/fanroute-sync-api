package com.fanroute.sync.domain.concert.batch;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import com.fanroute.sync.domain.concert.client.KopisClient;
import com.fanroute.sync.domain.concert.config.KopisProperties;
import com.fanroute.sync.domain.concert.dto.KopisDto;

import lombok.RequiredArgsConstructor;

/** KOPIS의 31일 조회 제한에 맞춰 기간을 나누고 공연 ID 기준으로 중복을 제거합니다. */
@RequiredArgsConstructor
public class ConcertItemReader implements ItemStreamReader<KopisDto.PerformanceSummary> {

  private static final DateTimeFormatter KOPIS_REQUEST_DATE_FORMAT = DateTimeFormatter.ofPattern(
      "yyyyMMdd");
  private static final int SYNC_PERIOD_MONTHS = 3;
  private static final int PAGE_SIZE = 100;
  private static final int MAX_DATE_RANGE_DAYS = 31;

  private final KopisClient kopisClient;
  private final KopisProperties kopisProperties;
  private final Clock clock;

  private Queue<KopisDto.PerformanceSummary> buffer;
  private Map<String, Boolean> seenConcertIds;
  private LocalDate windowStart;
  private LocalDate periodEnd;
  private int pageNo;
  private int returnedCount;

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    LocalDate today = LocalDate.now(clock);
    this.windowStart = today;
    this.periodEnd = today.plusMonths(SYNC_PERIOD_MONTHS);
    this.buffer = new ArrayDeque<>();
    this.seenConcertIds = new LinkedHashMap<>();
    this.pageNo = 1;
    this.returnedCount = 0;
  }

  @Override
  public KopisDto.PerformanceSummary read() {
    if (reachedSyncLimit()) {
      return null;
    }
    while (buffer.isEmpty() && windowStart != null && !windowStart.isAfter(periodEnd)) {
      fetchNextWindow();
    }
    KopisDto.PerformanceSummary next = buffer.poll();
    if (next != null) {
      returnedCount++;
    }
    return next;
  }

  /** 조회 성공 후 페이지나 구간을 이동해 재시도 시 같은 요청을 반복합니다. */
  private void fetchNextWindow() {
    LocalDate maxWindowEnd = windowStart.plusDays(MAX_DATE_RANGE_DAYS - 1);
    LocalDate windowEnd = maxWindowEnd.isAfter(periodEnd) ? periodEnd : maxWindowEnd;
    String startDate = windowStart.format(KOPIS_REQUEST_DATE_FORMAT);
    String endDate = windowEnd.format(KOPIS_REQUEST_DATE_FORMAT);

    int requestedItems = requestSize();
    KopisDto.PerformanceListResponse response = kopisClient.getPerformances(
        kopisProperties.serviceKey(), startDate, endDate, pageNo, requestedItems,
        kopisProperties.regionCode());
    var performances = response.performancesOrEmpty();
    for (KopisDto.PerformanceSummary summary : performances) {
      if (seenConcertIds.putIfAbsent(summary.kopisConcertId(), Boolean.TRUE) == null) {
        buffer.add(summary);
      }
    }
    if (performances.size() < requestedItems) {
      windowStart = windowEnd.plusDays(1);
      pageNo = 1;
    } else {
      pageNo++;
    }
  }

  private boolean reachedSyncLimit() {
    return kopisProperties.syncMaxItems() > 0
        && returnedCount >= kopisProperties.syncMaxItems();
  }

  private int requestSize() {
    if (kopisProperties.syncMaxItems() == 0) {
      return PAGE_SIZE;
    }
    return Math.min(PAGE_SIZE, kopisProperties.syncMaxItems() - returnedCount);
  }
}
