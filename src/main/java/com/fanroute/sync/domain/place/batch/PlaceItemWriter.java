package com.fanroute.sync.domain.place.batch;

import java.time.Clock;
import java.time.Instant;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.repository.PlaceRepository;

import lombok.RequiredArgsConstructor;

/** Spring Batch의 Chunk 트랜잭션 안에서 장소를 Upsert합니다. */
@RequiredArgsConstructor
public class PlaceItemWriter implements ItemWriter<TourApiDto.PlaceSummary> {

  private final PlaceRepository placeRepository;
  private final PlaceCategory category;
  private final Clock clock;

  @Override
  public void write(Chunk<? extends TourApiDto.PlaceSummary> chunk) {
    for (TourApiDto.PlaceSummary item : chunk.getItems()) {
      upsert(item);
    }
  }

  private void upsert(TourApiDto.PlaceSummary item) {
    Instant syncedAt = clock.instant();
    placeRepository.findByContentId(item.contentId())
        .ifPresentOrElse(
            place -> place.updateFromSync(
                category, item.contentTypeId(), item.name(), item.address(), item.detailAddress(),
                item.zipCode(), item.latitude(), item.longitude(), item.telephone(),
                item.imageUrl(), item.thumbnailUrl(), item.copyrightType(),
                item.legalDongRegionCode(), item.legalDongSignguCode(),
                item.classificationLevel1(), item.classificationLevel2(),
                item.classificationLevel3(), item.sourceCreatedAt(), item.sourceModifiedAt(),
                syncedAt),
            () -> placeRepository.save(Place.create(
                item.contentId(), category, item.contentTypeId(), item.name(), item.address(),
                item.detailAddress(), item.zipCode(), item.latitude(), item.longitude(),
                item.telephone(), item.imageUrl(), item.thumbnailUrl(), item.copyrightType(),
                item.legalDongRegionCode(), item.legalDongSignguCode(),
                item.classificationLevel1(), item.classificationLevel2(),
                item.classificationLevel3(), item.sourceCreatedAt(), item.sourceModifiedAt(),
                syncedAt)));
  }
}
