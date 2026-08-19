package com.fanroute.sync.domain.concert.entity;

import java.util.Arrays;

public enum Genre {

  PLAY("연극"),
  DANCE("무용(서양/한국무용)"),
  POPULAR_DANCE("대중무용"),
  WESTERN_MUSIC("서양음악(클래식)"),
  KOREAN_MUSIC("한국음악(국악)"),
  POPULAR_MUSIC("대중음악"),
  COMPLEX("복합"),
  CIRCUS_MAGIC("서커스/마술"),
  MUSICAL("뮤지컬");

  private final String label;

  Genre(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static Genre fromLabel(String label) {
    return Arrays.stream(values())
        .filter(genre -> genre.label.equals(label))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("알 수 없는 장르명: " + label));
  }
}
