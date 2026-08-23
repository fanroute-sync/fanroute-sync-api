package com.fanroute.sync.domain.schedule.entity;

import java.time.LocalTime;

public enum TravelTimeSlot {
  MORNING(LocalTime.of(9, 0)),
  AFTERNOON(LocalTime.of(13, 0)),
  EVENING(LocalTime.of(18, 0));

  private final LocalTime time;

  TravelTimeSlot(LocalTime time) {
    this.time = time;
  }

  public LocalTime getTime() {
    return time;
  }
}
