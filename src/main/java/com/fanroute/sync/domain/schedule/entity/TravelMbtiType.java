package com.fanroute.sync.domain.schedule.entity;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TravelMbtiType {
  FOOD_EXPLORER("맛집탐방형"),
  PHOTO_SENSIBILITY("감성사진형"),
  HISTORY_CULTURE("역사문화형"),
  NATURE_HEALING("자연힐링형"),
  SHOPPING_FOCUS("쇼핑집중형"),
  CAFE_TOUR("카페투어형"),
  ACTIVITY("액티비티형"),
  LOCAL_EXPERIENCE("로컬체험형"),
  CONCERT_FOCUS("공연몰입형");

  private final String label;

  TravelMbtiType(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  @JsonCreator
  public static TravelMbtiType from(String value) {
    return Arrays.stream(values())
        .filter(type -> type.name().equals(value) || type.label.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 여행 MBTI입니다: " + value));
  }
}
