package com.fanroute.sync.domain.schedule.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AiItineraryTimeWindowTest {

  @ParameterizedTest
  @CsvSource({
      "2026-09-01, 2026-09-01T09:00, 2026-09-02T00:00",
      "2026-09-02, 2026-09-02T00:00, 2026-09-03T00:00",
      "2026-09-03, 2026-09-03T00:00, 2026-09-03T18:00"
  })
  @DisplayName("한국 시간으로 도착일·중간일·출발일의 허용 범위를 계산한다")
  void clipsToLocalDay(String date, String start, String end) {
    AiItineraryGenerationDto.TimeWindow window = AiItineraryGenerationDto.TimeWindow.forDate(
        LocalDate.parse(date), Instant.parse("2026-09-01T00:00:00Z"),
        Instant.parse("2026-09-03T09:00:00Z"));

    assertThat(window.start()).isEqualTo(start);
    assertThat(window.end()).isEqualTo(end);
  }
}
