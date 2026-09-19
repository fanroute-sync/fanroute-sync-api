package com.fanroute.sync.domain.place.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.repository.PlaceRepository;

@ExtendWith(MockitoExtension.class)
class PlaceItemWriterTest {

  @Mock
  private PlaceRepository placeRepository;

  private PlaceItemWriter writer;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneOffset.UTC);
    writer = new PlaceItemWriter(placeRepository, PlaceCategory.ACCOMMODATION, clock);
  }

  @Test
  @DisplayName("신규 contentId는 새로 저장한다")
  void savesNewPlace() {
    when(placeRepository.findByContentId("126508")).thenReturn(Optional.empty());

    writer.write(new Chunk<>(summary("126508")));

    ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);
    verify(placeRepository).save(captor.capture());
    assertThat(captor.getValue().getContentId()).isEqualTo("126508");
    assertThat(captor.getValue().getCategory()).isEqualTo(PlaceCategory.ACCOMMODATION);
    assertThat(captor.getValue().getLastSyncedAt())
        .isEqualTo(Instant.parse("2026-08-19T00:00:00Z"));
  }

  @Test
  @DisplayName("이미 저장된 contentId는 새로 저장하지 않고 갱신한다")
  void updatesExistingPlace() {
    Place existing = Place.create(
        "126508", PlaceCategory.ACCOMMODATION, "32", "옛 이름", "옛 주소", null, null, 0.0, 0.0, null,
        null, null, null, null, null, null, null, null, null, null,
        Instant.parse("2020-01-01T00:00:00Z"));
    when(placeRepository.findByContentId("126508")).thenReturn(Optional.of(existing));

    writer.write(new Chunk<>(summary("126508")));

    verify(placeRepository, never()).save(any());
    assertThat(existing.getName()).isEqualTo("테스트 장소");
    assertThat(existing.getLastSyncedAt()).isEqualTo(Instant.parse("2026-08-19T00:00:00Z"));
  }

  private TourApiDto.PlaceSummary summary(String contentId) {
    return new TourApiDto.PlaceSummary(
        contentId, "32", "테스트 장소", "부산", null, null, 129.0, 35.1, null, null, null, null, "26",
        null, null, null, null, null, null, null);
  }
}
