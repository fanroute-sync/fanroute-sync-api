package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceTag;
import com.fanroute.sync.domain.schedule.entity.Accommodation;

class AiPlaceCandidateRankerTest {

  private final AiPlaceCandidateRanker ranker = new AiPlaceCandidateRanker();

  @Test
  @DisplayName("숙소에서 먼 장소도 제외하지 않고 거리와 여행 스타일 점수로 정렬한다")
  void ranksPlacesByDistanceAndStyleWithoutExcludingDistantPlaces() {
    Accommodation accommodation = accommodation(35.1587, 129.1604);
    Place nearbyCafe = place(1L, "가까운 카페", 35.1590, 129.1604, Set.of(PlaceTag.CAFE));
    Place nearbyRestaurant = place(2L, "가까운 식당", 35.1592, 129.1604, Set.of(PlaceTag.FOOD));
    Place distantRestaurant = place(3L, "먼 식당", 35.4000, 129.1604, Set.of(PlaceTag.FOOD));

    List<Place> result = ranker.rank(List.of(distantRestaurant, nearbyRestaurant, nearbyCafe),
        Set.of(), List.of(accommodation), "맛집탐방형", List.of());

    assertThat(result).extracting(Place::getId).containsExactly(2L, 1L, 3L);
  }

  @Test
  @DisplayName("기존 일정 장소는 숙소 좌표가 없어도 후보에서 제외한다")
  void excludesExistingPlacesWithoutAccommodationAnchor() {
    Place existingPlace = place(1L, "기존 장소", 35.1587, 129.1604, Set.of(PlaceTag.FOOD));
    Place candidatePlace = place(2L, "새 장소", 35.1588, 129.1604, Set.of(PlaceTag.CAFE));

    List<Place> result = ranker.rank(List.of(existingPlace, candidatePlace), Set.of(1L),
        List.of(), null, List.of("연인"));

    assertThat(result).extracting(Place::getId).containsExactly(2L);
  }

  private Accommodation accommodation(double latitude, double longitude) {
    Accommodation accommodation = org.mockito.Mockito.mock(Accommodation.class);
    when(accommodation.getLatitude()).thenReturn(latitude);
    when(accommodation.getLongitude()).thenReturn(longitude);
    when(accommodation.getCheckinDate()).thenReturn(LocalDate.of(2026, 9, 1));
    return accommodation;
  }

  private Place place(Long id, String name, double latitude, double longitude, Set<PlaceTag> tags) {
    Place place = org.mockito.Mockito.mock(Place.class);
    when(place.getId()).thenReturn(id);
    when(place.getName()).thenReturn(name);
    when(place.getLatitude()).thenReturn(latitude);
    when(place.getLongitude()).thenReturn(longitude);
    when(place.getTags()).thenReturn(tags);
    return place;
  }
}
