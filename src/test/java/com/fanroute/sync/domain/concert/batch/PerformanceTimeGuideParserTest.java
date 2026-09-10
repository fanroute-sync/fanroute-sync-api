package com.fanroute.sync.domain.concert.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fanroute.sync.domain.concert.batch.PerformanceTimeGuideParser.GeneratedSlot;
import com.fanroute.sync.domain.concert.batch.PerformanceTimeGuideParser.GenerationResult;
import com.fanroute.sync.domain.concert.entity.ScheduleParseStatus;

class PerformanceTimeGuideParserTest {

  @Test
  @DisplayName("KOPIS 실제 안내 샘플을 한 주 전체에 대해 정확히 전개한다")
  void parsesRealKopisSampleAcrossOneWeek() {
    String guide = "화요일 ~ 금요일(20:00), 토요일(16:00,19:00), 일요일(15:00,18:00)";
    // 2026-09-07(월)~09-13(일) — 월요일은 안내에 없으므로 제외돼야 한다.
    GenerationResult result = PerformanceTimeGuideParser.generate(
        guide, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13));

    assertThat(result.status()).isEqualTo(ScheduleParseStatus.PARSED);
    assertThat(result.slots()).containsExactly(
        new GeneratedSlot(LocalDate.of(2026, 9, 8), LocalTime.of(20, 0)), // 화
        new GeneratedSlot(LocalDate.of(2026, 9, 9), LocalTime.of(20, 0)), // 수
        new GeneratedSlot(LocalDate.of(2026, 9, 10), LocalTime.of(20, 0)), // 목
        new GeneratedSlot(LocalDate.of(2026, 9, 11), LocalTime.of(20, 0)), // 금
        new GeneratedSlot(LocalDate.of(2026, 9, 12), LocalTime.of(16, 0)), // 토
        new GeneratedSlot(LocalDate.of(2026, 9, 12), LocalTime.of(19, 0)), // 토
        new GeneratedSlot(LocalDate.of(2026, 9, 13), LocalTime.of(15, 0)), // 일
        new GeneratedSlot(LocalDate.of(2026, 9, 13), LocalTime.of(18, 0))); // 일
  }

  @Test
  @DisplayName("단일 요일·단일 시각도 해석한다")
  void parsesSingleDaySingleTime() {
    GenerationResult result = PerformanceTimeGuideParser.generate(
        "화요일(20:00)", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2));

    assertThat(result.status()).isEqualTo(ScheduleParseStatus.PARSED);
    assertThat(result.slots())
        .containsExactly(new GeneratedSlot(LocalDate.of(2026, 9, 1), LocalTime.of(20, 0)));
  }

  @Test
  @DisplayName("요일 범위가 겹쳐도 같은 날짜와 시각의 회차는 한 번만 생성한다")
  void removesDuplicateSlotsFromOverlappingSegments() {
    GenerationResult result = PerformanceTimeGuideParser.generate(
        "화요일 ~ 금요일(20:00), 금요일(20:00)",
        LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 11));

    assertThat(result.status()).isEqualTo(ScheduleParseStatus.PARSED);
    assertThat(result.slots()).containsExactly(
        new GeneratedSlot(LocalDate.of(2026, 9, 8), LocalTime.of(20, 0)),
        new GeneratedSlot(LocalDate.of(2026, 9, 9), LocalTime.of(20, 0)),
        new GeneratedSlot(LocalDate.of(2026, 9, 10), LocalTime.of(20, 0)),
        new GeneratedSlot(LocalDate.of(2026, 9, 11), LocalTime.of(20, 0)));
  }

  @Test
  @DisplayName("원문이 없거나 공백뿐이면 NO_SCHEDULE이다")
  void returnsNoScheduleWhenGuideIsBlank() {
    assertThat(PerformanceTimeGuideParser.generate(null, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 2)).status()).isEqualTo(ScheduleParseStatus.NO_SCHEDULE);
    assertThat(PerformanceTimeGuideParser.generate("  ", LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 2)).status()).isEqualTo(ScheduleParseStatus.NO_SCHEDULE);
  }

  @Test
  @DisplayName("알려진 패턴 밖의 자유 텍스트는 UNSUPPORTED_FORMAT으로 실패 처리한다")
  void returnsUnsupportedFormatForFreeText() {
    GenerationResult result = PerformanceTimeGuideParser.generate(
        "홈페이지 참고", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2));

    assertThat(result.status()).isEqualTo(ScheduleParseStatus.UNSUPPORTED_FORMAT);
    assertThat(result.slots()).isEmpty();
  }

  @Test
  @DisplayName("세그먼트 뒤에 알 수 없는 문구가 붙으면 부분 성공을 인정하지 않고 전체 실패로 처리한다")
  void treatsTrailingGarbageAsFullFailure() {
    GenerationResult result = PerformanceTimeGuideParser.generate(
        "화요일(20:00) 문의", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2));

    assertThat(result.status()).isEqualTo(ScheduleParseStatus.UNSUPPORTED_FORMAT);
  }

  @Test
  @DisplayName("요일 범위가 거꾸로면(주 경계를 넘는 표현) 지원하지 않는 형식으로 처리한다")
  void treatsReversedDayRangeAsUnsupported() {
    GenerationResult result = PerformanceTimeGuideParser.generate(
        "금요일 ~ 화요일(20:00)", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

    assertThat(result.status()).isEqualTo(ScheduleParseStatus.UNSUPPORTED_FORMAT);
  }

  @Test
  @DisplayName("전개 결과가 상한을 넘으면 전체를 실패로 처리한다")
  void returnsLimitExceededWhenTooManySlotsGenerated() {
    GenerationResult result = PerformanceTimeGuideParser.generate(
        "월요일 ~ 일요일(20:00)", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)); // 365일

    assertThat(result.status()).isEqualTo(ScheduleParseStatus.LIMIT_EXCEEDED);
    assertThat(result.slots()).isEmpty();
  }

  @Test
  @DisplayName("해시는 원문·시작일·종료일이 모두 같을 때만 같다")
  void hashDependsOnGuideAndDateRange() {
    String hash1 = PerformanceTimeGuideParser.hash(
        "화요일(20:00)", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2));
    String hash2 = PerformanceTimeGuideParser.hash(
        "화요일(20:00)", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2));
    String hashDifferentGuide = PerformanceTimeGuideParser.hash(
        "화요일(19:00)", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2));
    String hashDifferentRange = PerformanceTimeGuideParser.hash(
        "화요일(20:00)", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3));

    assertThat(hash1).isEqualTo(hash2);
    assertThat(hash1).isNotEqualTo(hashDifferentGuide);
    assertThat(hash1).isNotEqualTo(hashDifferentRange);
  }

  @Test
  @DisplayName("빈 문자열 세그먼트가 하나도 없으면 UNSUPPORTED_FORMAT이다")
  void returnsUnsupportedFormatWhenNoSegmentMatches() {
    List<GeneratedSlot> slots = PerformanceTimeGuideParser.generate(
        "20:00", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2)).slots();

    assertThat(slots).isEmpty();
  }
}
