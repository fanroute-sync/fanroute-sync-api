package com.fanroute.sync.domain.place.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaceTest {

  @Test
  @DisplayName("장소를 생성하면 입력한 값이 그대로 저장된다")
  void createsPlaceWithGivenValues() {
    Instant syncedAt = Instant.parse("2026-08-19T00:00:00Z");

    Place place = Place.create(
        "126508", PlaceCategory.ACCOMMODATION, "32", "테스트 호텔", "부산 해운대구", "101호", "48058",
        35.163, 129.163, "051-000-0000", "image.jpg", "thumb.jpg", "Type3", "26", "26350", "AC",
        "AC01", "AC01010100", "20260101120000", "20260102120000", syncedAt);

    assertThat(place.getContentId()).isEqualTo("126508");
    assertThat(place.getCategory()).isEqualTo(PlaceCategory.ACCOMMODATION);
    assertThat(place.getContentTypeId()).isEqualTo("32");
    assertThat(place.getName()).isEqualTo("테스트 호텔");
    assertThat(place.getAddress()).isEqualTo("부산 해운대구");
    assertThat(place.getDetailAddress()).isEqualTo("101호");
    assertThat(place.getZipCode()).isEqualTo("48058");
    assertThat(place.getLatitude()).isEqualTo(35.163);
    assertThat(place.getLongitude()).isEqualTo(129.163);
    assertThat(place.getTelephone()).isEqualTo("051-000-0000");
    assertThat(place.getImageUrl()).isEqualTo("image.jpg");
    assertThat(place.getThumbnailUrl()).isEqualTo("thumb.jpg");
    assertThat(place.getCopyrightType()).isEqualTo("Type3");
    assertThat(place.getSource()).isEqualTo(Place.SOURCE_KTO_TOUR_API);
    assertThat(place.getLegalDongRegionCode()).isEqualTo("26");
    assertThat(place.getLegalDongSignguCode()).isEqualTo("26350");
    assertThat(place.getClassificationLevel1()).isEqualTo("AC");
    assertThat(place.getSourceCreatedAt()).isEqualTo("20260101120000");
    assertThat(place.getSourceModifiedAt()).isEqualTo("20260102120000");
    assertThat(place.getLastSyncedAt()).isEqualTo(syncedAt);
  }

  @Test
  @DisplayName("재동기화하면 식별자를 제외한 정보가 최신값으로 갱신된다")
  void updatesFromSync() {
    Instant firstSync = Instant.parse("2026-08-19T00:00:00Z");
    Instant secondSync = Instant.parse("2026-08-20T00:00:00Z");
    Place place = Place.create(
        "126508", PlaceCategory.ACCOMMODATION, "32", "옛 이름", "옛 주소", null, null, 0.0, 0.0, null,
        null, null, null, null, null, null, null, null, null, null, firstSync);

    place.updateFromSync(
        PlaceCategory.ACCOMMODATION, "32", "새 이름", "새 주소", "202호", "48059", 35.2, 129.2,
        "051-111-1111", "new-image.jpg", "new-thumb.jpg", "Type1", "26", "26380", "AC", "AC02",
        "AC02010100", "20260103120000", "20260104120000", secondSync);

    assertThat(place.getContentId()).isEqualTo("126508");
    assertThat(place.getCategory()).isEqualTo(PlaceCategory.ACCOMMODATION);
    assertThat(place.getName()).isEqualTo("새 이름");
    assertThat(place.getAddress()).isEqualTo("새 주소");
    assertThat(place.getDetailAddress()).isEqualTo("202호");
    assertThat(place.getZipCode()).isEqualTo("48059");
    assertThat(place.getLatitude()).isEqualTo(35.2);
    assertThat(place.getLongitude()).isEqualTo(129.2);
    assertThat(place.getLegalDongSignguCode()).isEqualTo("26380");
    assertThat(place.getLastSyncedAt()).isEqualTo(secondSync);
    assertThat(place.getSource()).isEqualTo(Place.SOURCE_KTO_TOUR_API);
  }

  @Test
  @DisplayName("외부 분류가 바뀌면 카테고리도 최신값으로 갱신된다")
  void updatesCategoryWhenExternalClassificationChanges() {
    Place place = Place.create(
        "126508", PlaceCategory.ACCOMMODATION, "32", "옛 이름", "옛 주소", null, null, 0.0, 0.0, null,
        null, null, null, null, null, null, null, null, null, null,
        Instant.parse("2026-08-19T00:00:00Z"));

    place.updateFromSync(
        PlaceCategory.ATTRACTION, "12", "옛 이름", "옛 주소", null, null, 0.0, 0.0, null, null, null,
        null, null, null, null, null, null, null, null, Instant.parse("2026-08-20T00:00:00Z"));

    assertThat(place.getCategory()).isEqualTo(PlaceCategory.ATTRACTION);
    assertThat(place.getContentTypeId()).isEqualTo("12");
  }
}
