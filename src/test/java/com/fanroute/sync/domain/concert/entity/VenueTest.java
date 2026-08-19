package com.fanroute.sync.domain.concert.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VenueTest {

  @Test
  @DisplayName("공연장을 생성하면 입력한 값이 그대로 저장된다")
  void createsVenueWithGivenValues() {
    Venue venue = Venue.create("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0);

    assertThat(venue.getKopisVenueId()).isEqualTo("FC001");
    assertThat(venue.getName()).isEqualTo("테스트홀");
    assertThat(venue.getAddress()).isEqualTo("부산 해운대구");
    assertThat(venue.getLatitude()).isEqualTo(35.1);
    assertThat(venue.getLongitude()).isEqualTo(129.0);
  }

  @Test
  @DisplayName("재동기화하면 공연장 정보가 최신값으로 갱신된다")
  void updatesFromSync() {
    Venue venue = Venue.create("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0);

    venue.updateFromSync("변경된 홀", "부산 사상구", 35.2, 129.1);

    assertThat(venue.getName()).isEqualTo("변경된 홀");
    assertThat(venue.getAddress()).isEqualTo("부산 사상구");
    assertThat(venue.getLatitude()).isEqualTo(35.2);
    assertThat(venue.getLongitude()).isEqualTo(129.1);
    assertThat(venue.getKopisVenueId()).isEqualTo("FC001");
  }
}
