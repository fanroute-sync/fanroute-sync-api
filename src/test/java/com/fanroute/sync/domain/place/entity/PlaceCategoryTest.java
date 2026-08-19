package com.fanroute.sync.domain.place.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaceCategoryTest {

  @Test
  @DisplayName("TourAPI contentTypeId로 PlaceCategory를 찾는다")
  void findsCategoryByContentTypeId() {
    assertThat(PlaceCategory.fromContentTypeId("32")).isEqualTo(PlaceCategory.ACCOMMODATION);
    assertThat(PlaceCategory.fromContentTypeId("12")).isEqualTo(PlaceCategory.ATTRACTION);
    assertThat(PlaceCategory.fromContentTypeId("39")).isEqualTo(PlaceCategory.RESTAURANT);
  }

  @Test
  @DisplayName("알 수 없는 contentTypeId면 예외가 발생한다")
  void throwsForUnknownContentTypeId() {
    assertThatThrownBy(() -> PlaceCategory.fromContentTypeId("15"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
