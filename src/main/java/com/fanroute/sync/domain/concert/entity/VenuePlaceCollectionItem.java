package com.fanroute.sync.domain.concert.entity;

import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "venue_place_collection_items", uniqueConstraints = {
    @UniqueConstraint(name = "uk_collection_items_collection_place",
        columnNames = {"collection_id", "place_id"}),
    @UniqueConstraint(name = "uk_collection_items_collection_sort_order",
        columnNames = {"collection_id", "sort_order"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenuePlaceCollectionItem extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "collection_id", nullable = false)
  private VenuePlaceCollection collection;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  private VenuePlaceCollectionItem(VenuePlaceCollection collection, Place place, int sortOrder) {
    this.collection = collection;
    this.place = place;
    this.sortOrder = sortOrder;
  }

  public static VenuePlaceCollectionItem create(VenuePlaceCollection collection, Place place,
      int sortOrder) {
    return new VenuePlaceCollectionItem(collection, place, sortOrder);
  }
}
