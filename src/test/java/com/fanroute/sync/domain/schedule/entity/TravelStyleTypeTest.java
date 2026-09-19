package com.fanroute.sync.domain.schedule.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TravelStyleTypeTest {

  @Test
  @DisplayName("여행 스타일 enum은 프론트 라벨과 영문 이름을 모두 읽는다")
  void readsLabelsAndEnumNames() {
    assertThat(CompanionType.from("친구")).isEqualTo(CompanionType.FRIEND);
    assertThat(CompanionType.from("CHILD")).isEqualTo(CompanionType.CHILD);
    assertThat(TravelMbtiType.from("맛집탐방형")).isEqualTo(TravelMbtiType.FOOD_EXPLORER);
    assertThat(TravelMbtiType.from("CAFE_TOUR")).isEqualTo(TravelMbtiType.CAFE_TOUR);
  }
}
