package com.fanroute.sync.domain.schedule.entity;

public enum AiItineraryGenerationStatus {
  PENDING,
  PROCESSING,
  COMPLETED,
  FAILED,
  CANCELLED;

  public boolean isTerminal() {
    return this == COMPLETED || this == FAILED || this == CANCELLED;
  }
}
