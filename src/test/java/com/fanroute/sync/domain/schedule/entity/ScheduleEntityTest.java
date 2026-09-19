package com.fanroute.sync.domain.schedule.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fanroute.sync.support.UserFixture;

class ScheduleEntityTest {

  @Test
  @DisplayName("여행 계획을 생성하면 일정 정보와 여행 스타일이 저장된다")
  void createsTripPlanWithScheduleAndTravelStyle() {
    TripPlan tripPlan = TripPlan.create(
        UserFixture.activeUser(), null,
        Instant.parse("2026-09-01T01:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        TravelIntensityType.RELAXED, List.of(CompanionType.FRIEND, CompanionType.CHILD), List.of("맛집", "관광"));

    assertThat(tripPlan.getConcert()).isNull();
    assertThat(tripPlan.getArrivalAt()).isEqualTo(Instant.parse("2026-09-01T01:00:00Z"));
    assertThat(tripPlan.getDepartureAt()).isEqualTo(Instant.parse("2026-09-03T09:00:00Z"));
    assertThat(tripPlan.getTravelIntensity()).isEqualTo(TravelIntensityType.RELAXED);
    assertThat(tripPlan.getCompanions()).containsExactly(CompanionType.FRIEND, CompanionType.CHILD);
    assertThat(tripPlan.getPreferences()).containsExactly("맛집", "관광");
  }

  @Test
  @DisplayName("여행 계획에 숙박과 날짜별 사용자 일정 항목을 생성할 수 있다")
  void createsAccommodationAndCustomItineraryItem() {
    TripPlan tripPlan = TripPlan.create(
        UserFixture.activeUser(), null,
        Instant.parse("2026-09-01T01:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        null, List.of(), List.of());
    Accommodation accommodation = Accommodation.create(
        tripPlan, "해운대 테스트 호텔", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3));
    ItineraryDay itineraryDay = ItineraryDay.create(tripPlan, LocalDate.of(2026, 9, 1), false);
    ItineraryItem item = ItineraryItem.create(
        itineraryDay, 1, LocalTime.of(13, 0), ItineraryItemType.CUSTOM, null, null,
        "점심 식사", 60);

    assertThat(accommodation.getTripPlan()).isSameAs(tripPlan);
    assertThat(accommodation.getNameOrAddress()).isEqualTo("해운대 테스트 호텔");
    assertThat(accommodation.getCheckinDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(accommodation.getCheckoutDate()).isEqualTo(LocalDate.of(2026, 9, 3));
    assertThat(itineraryDay.isConcertDay()).isFalse();
    assertThat(item.getItineraryDay()).isSameAs(itineraryDay);
    assertThat(item.getType()).isEqualTo(ItineraryItemType.CUSTOM);
    assertThat(item.getTitle()).isEqualTo("점심 식사");
    assertThat(item.getDurationMinutes()).isEqualTo(60);
  }

  @Test
  @DisplayName("AI 생성 종료 상태는 processing lease를 제거한다")
  void clearsProcessingLeaseWhenGenerationCompletes() {
    AiItineraryGeneration generation = AiItineraryGeneration.create(
        ItineraryDay.create(TripPlan.create(UserFixture.activeUser(), null,
            Instant.parse("2026-09-01T01:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
            null, List.of(), List.of()), LocalDate.of(2026, 9, 1), false));
    generation.start(Instant.parse("2026-09-01T01:01:00Z"));

    generation.complete();

    assertThat(generation.getStatus()).isEqualTo(AiItineraryGenerationStatus.COMPLETED);
    assertThat(generation.getProcessingLeaseUntil()).isNull();
    assertThat(generation.getStatus().isTerminal()).isTrue();
  }

  @Test
  @DisplayName("AI 생성 재시도는 다음 시각과 마지막 실패 이유를 기록한다")
  void schedulesAiGenerationRetry() {
    AiItineraryGeneration generation = AiItineraryGeneration.create(
        ItineraryDay.create(TripPlan.create(UserFixture.activeUser(), null,
            Instant.parse("2026-09-01T01:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
            null, List.of(), List.of()), LocalDate.of(2026, 9, 1), false));
    generation.start(Instant.parse("2026-09-01T01:01:00Z"));
    Instant nextAttemptAt = Instant.parse("2026-09-01T01:02:00Z");

    generation.scheduleRetry(nextAttemptAt, "429 Too Many Requests");

    assertThat(generation.getStatus()).isEqualTo(AiItineraryGenerationStatus.PENDING);
    assertThat(generation.getProcessingLeaseUntil()).isNull();
    assertThat(generation.getNextAttemptAt()).isEqualTo(nextAttemptAt);
    assertThat(generation.getLastFailureReason()).isEqualTo("429 Too Many Requests");
  }
}
