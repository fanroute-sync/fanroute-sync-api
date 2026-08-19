package com.fanroute.sync.domain.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.support.AbstractRepositoryTest;

class PlaceRepositoryTest extends AbstractRepositoryTest {

  @Autowired
  private PlaceRepository placeRepository;

  @Test
  @DisplayName("contentId로 장소를 조회한다")
  void findsByContentId() {
    placeRepository.saveAndFlush(createPlace("126508", PlaceCategory.ACCOMMODATION));

    assertThat(placeRepository.findByContentId("126508")).isPresent();
    assertThat(placeRepository.findByContentId("없는ID")).isEmpty();
  }

  @Test
  @DisplayName("카테고리로 필터링해서 조회한다")
  void findsByCategory() {
    placeRepository.saveAndFlush(createPlace("126508", PlaceCategory.ACCOMMODATION));
    placeRepository.saveAndFlush(createPlace("126509", PlaceCategory.ATTRACTION));

    Page<Place> page = placeRepository.findByCategory(
        PlaceCategory.ACCOMMODATION, PageRequest.of(0, 20));

    assertThat(page.getContent()).hasSize(1);
    assertThat(page.getContent().get(0).getContentId()).isEqualTo("126508");
  }

  private Place createPlace(String contentId, PlaceCategory category) {
    return Place.create(
        contentId, category, "32", "테스트 장소", "부산", null, null, 35.1, 129.0, null, null, null,
        null, "26", null, null, null, null, null, null, Instant.now());
  }
}
