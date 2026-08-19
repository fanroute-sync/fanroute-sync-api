package com.fanroute.sync.domain.concert.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GenreTest {

  @Test
  @DisplayName("KOPIS 장르 라벨로 Genre를 찾는다")
  void findsGenreByLabel() {
    assertThat(Genre.fromLabel("대중음악")).isEqualTo(Genre.POPULAR_MUSIC);
    assertThat(Genre.fromLabel("무용(서양/한국무용)")).isEqualTo(Genre.DANCE);
  }

  @Test
  @DisplayName("알 수 없는 장르 라벨이면 예외가 발생한다")
  void throwsForUnknownLabel() {
    assertThatThrownBy(() -> Genre.fromLabel("알수없는장르"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
