package com.fanroute.sync.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.domain.place.repository.PlaceRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

  @Mock
  private PlaceRepository placeRepository;

  private PlaceService placeService;

  @BeforeEach
  void setUp() {
    placeService = new PlaceService(placeRepository);
  }

  @Test
  @DisplayName("카테고리 없이 조회하면 전체 장소를 조회한다")
  void getsAllPlacesWithoutCategory() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Place> page = new PageImpl<>(List.of());
    when(placeRepository.findAll(pageable)).thenReturn(page);

    Page<Place> result = placeService.getPlaces(null, pageable);

    assertThat(result).isSameAs(page);
  }

  @Test
  @DisplayName("카테고리가 주어지면 해당 카테고리만 필터링해서 조회한다")
  void getsPlacesFilteredByCategory() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Place> page = new PageImpl<>(List.of());
    when(placeRepository.findByCategory(PlaceCategory.ACCOMMODATION, pageable))
        .thenReturn(page);

    Page<Place> result = placeService.getPlaces(PlaceCategory.ACCOMMODATION, pageable);

    assertThat(result).isSameAs(page);
  }

  @Test
  @DisplayName("존재하는 장소를 ID로 조회한다")
  void getsPlaceById() {
    Place place = Place.create(
        "126508", PlaceCategory.ACCOMMODATION, "32", "테스트 호텔", "부산", null, null, 35.1, 129.0,
        null, null, null, null, "26", null, null, null, null, null, null, Instant.now());
    when(placeRepository.findById(1L)).thenReturn(Optional.of(place));

    assertThat(placeService.getPlace(1L)).isSameAs(place);
  }

  @Test
  @DisplayName("존재하지 않는 장소를 조회하면 예외가 발생한다")
  void throwsWhenPlaceNotFound() {
    when(placeRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> placeService.getPlace(1L))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND));
  }
}
