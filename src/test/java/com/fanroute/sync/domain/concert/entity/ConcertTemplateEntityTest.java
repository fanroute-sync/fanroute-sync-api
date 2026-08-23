package com.fanroute.sync.domain.concert.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConcertTemplateEntityTest {

  @Test
  @DisplayName("공연별 추천 일정 템플릿과 항목을 생성할 수 있다")
  void createsConcertTemplateAndItem() {
    Venue venue = Venue.create("MT10TEST", "테스트 공연장", "부산광역시", 35.1, 129.1);
    Concert concert = Concert.create(
        "MT20TEST", venue, "테스트 콘서트", Genre.POPULAR_MUSIC,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1), null,
        java.time.Instant.parse("2026-08-01T00:00:00Z"));
    ConcertTemplate template = ConcertTemplate.create(
        concert, "공연 전 식사 코스", "공연장 인근 식사 후 입장하는 추천 코스입니다.");
    ConcertTemplateItem item = ConcertTemplateItem.create(
        template, 1, LocalTime.of(17, 30), "공연장 인근 식사", 60);

    assertThat(template.getConcert()).isSameAs(concert);
    assertThat(template.getSummary()).isEqualTo("공연 전 식사 코스");
    assertThat(item.getConcertTemplate()).isSameAs(template);
    assertThat(item.getSortOrder()).isEqualTo(1);
    assertThat(item.getScheduledTime()).isEqualTo(LocalTime.of(17, 30));
    assertThat(item.getTitle()).isEqualTo("공연장 인근 식사");
    assertThat(item.getDurationMinutes()).isEqualTo(60);
  }
}
