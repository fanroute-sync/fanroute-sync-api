package com.fanroute.sync.domain.place.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.global.common.exception.BusinessException;

class PlaceItemProcessorTest {

  private final PlaceItemProcessor processor = new PlaceItemProcessor(PlaceCategory.ACCOMMODATION);

  @Test
  @DisplayName("contentTypeId가 요청 카테고리와 일치하면 그대로 통과시킨다")
  void passesThroughMatchingCategory() {
    TourApiDto.PlaceSummary item = summary("1", "32");

    assertThat(processor.process(item)).isSameAs(item);
  }

  @Test
  @DisplayName("contentTypeId가 요청 카테고리와 다르면 null을 반환해 걸러낸다")
  void filtersOutMismatchedCategory() {
    TourApiDto.PlaceSummary item = summary("1", "12");

    assertThat(processor.process(item)).isNull();
  }

  @Test
  @DisplayName("contentId가 없으면 응답 오류로 처리한다")
  void rejectsBlankContentId() {
    TourApiDto.PlaceSummary item = summary("", "32");

    assertThatThrownBy(() -> processor.process(item))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(PlaceErrorCode.TOUR_API_RESPONSE_INVALID));
  }

  private TourApiDto.PlaceSummary summary(String contentId, String contentTypeId) {
    return new TourApiDto.PlaceSummary(
        contentId, contentTypeId, "테스트 장소", "부산", null, null, 129.0, 35.1, null, null, null,
        null, "26", null, null, null, null, null, null, null);
  }
}
