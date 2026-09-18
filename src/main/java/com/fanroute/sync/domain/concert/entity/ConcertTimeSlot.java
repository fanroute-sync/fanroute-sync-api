package com.fanroute.sync.domain.concert.entity;

import java.time.LocalTime;

/** 공연 시작 시각을 하루 중 대략적인 시간대로 분류합니다. 추천 장소가 어느 시간대에
 * 어울리는지 큐레이터가 태그하는 기준이며, {@link #fromTime}으로 공연 자체의 시간대도 같은
 * 기준으로 계산합니다. */
public enum ConcertTimeSlot {
  MORNING,
  LUNCH,
  EVENING,
  NIGHT;

  private static final LocalTime MORNING_START = LocalTime.of(5, 0);
  private static final LocalTime LUNCH_START = LocalTime.of(11, 0);
  private static final LocalTime EVENING_START = LocalTime.of(15, 0);
  private static final LocalTime NIGHT_START = LocalTime.of(20, 0);

  public static ConcertTimeSlot fromTime(LocalTime time) {
    if (isBetween(time, MORNING_START, LUNCH_START)) {
      return MORNING;
    }
    if (isBetween(time, LUNCH_START, EVENING_START)) {
      return LUNCH;
    }
    if (isBetween(time, EVENING_START, NIGHT_START)) {
      return EVENING;
    }
    return NIGHT;
  }

  private static boolean isBetween(LocalTime time, LocalTime startInclusive, LocalTime endExclusive) {
    return !time.isBefore(startInclusive) && time.isBefore(endExclusive);
  }
}
