package com.fanroute.sync.domain.concert.entity;

/** PARSED일 때만 자동 회차를 생성·교체합니다. 그 외 상태는 기존 자동·미확정 회차를 비활성화합니다. */
public enum ScheduleParseStatus {
  NOT_PARSED,
  PARSED,
  UNSUPPORTED_FORMAT,
  NO_SCHEDULE,
  LIMIT_EXCEEDED
}
