package com.fanroute.sync.domain.schedule.entity;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CompanionType {
  FRIEND("친구"),
  PARTNER("연인"),
  SOLO("혼자"),
  PARENT("부모님"),
  CHILD("아이");

  private final String label;

  CompanionType(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  @JsonCreator
  public static CompanionType from(String value) {
    return Arrays.stream(values())
        .filter(type -> type.name().equals(value) || type.label.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 동행 유형입니다: " + value));
  }
}
