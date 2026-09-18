package com.fanroute.sync.domain.place.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.fanroute.sync.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** TourAPI 장소 유형에 공통으로 필요한 지도·일정 후보 정보를 저장합니다. */
@Getter
@Entity
@Table(name = "places", uniqueConstraints = {
    @UniqueConstraint(name = "uk_places_content_id", columnNames = "content_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {

  public static final String SOURCE_KTO_TOUR_API = "KTO_TOUR_API";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "content_id", nullable = false, length = 20)
  private String contentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PlaceCategory category;

  @Column(name = "content_type_id", nullable = false, length = 10)
  private String contentTypeId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 300)
  private String address;

  @Column(name = "detail_address", length = 300)
  private String detailAddress;

  @Column(name = "zip_code", length = 10)
  private String zipCode;

  private Double latitude;

  private Double longitude;

  @Column(length = 30)
  private String telephone;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Column(name = "thumbnail_url", length = 500)
  private String thumbnailUrl;

  @Column(name = "copyright_type", length = 10)
  private String copyrightType; // 이미지 저작권 유형(cpyrhtDivCd) — null/blank는 "권리 미확인"이지 Type1이 아님

  @Column(nullable = false, length = 20)
  private String source; // 데이터 출처. 항상 SOURCE_KTO_TOUR_API — 저작권 조건 추적을 위해 유지

  @Column(name = "legal_dong_region_code", length = 10)
  private String legalDongRegionCode;

  @Column(name = "legal_dong_signgu_code", length = 10)
  private String legalDongSignguCode;

  @Column(name = "classification_level1", length = 20)
  private String classificationLevel1;

  @Column(name = "classification_level2", length = 20)
  private String classificationLevel2;

  @Column(name = "classification_level3", length = 20)
  private String classificationLevel3;

  @Column(name = "source_created_at", length = 20)
  private String sourceCreatedAt;

  @Column(name = "source_modified_at", length = 20)
  private String sourceModifiedAt;

  @Column(name = "last_synced_at", nullable = false)
  private Instant lastSyncedAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "place_tags", joinColumns = @JoinColumn(name = "place_id"),
      uniqueConstraints = @UniqueConstraint(
          name = "uk_place_tags_place_id_tag", columnNames = {"place_id", "tag"}))
  @Enumerated(EnumType.STRING)
  @Column(name = "tag", nullable = false, length = 30)
  private Set<PlaceTag> tags = new HashSet<>();

  private Place(String contentId, PlaceCategory category, String contentTypeId, String name,
      String address, String detailAddress, String zipCode, Double latitude, Double longitude,
      String telephone, String imageUrl, String thumbnailUrl, String copyrightType,
      String legalDongRegionCode, String legalDongSignguCode, String classificationLevel1,
      String classificationLevel2, String classificationLevel3, String sourceCreatedAt,
      String sourceModifiedAt, Instant lastSyncedAt) {
    this.contentId = contentId;
    this.category = category;
    this.contentTypeId = contentTypeId;
    this.name = name;
    this.address = address;
    this.detailAddress = detailAddress;
    this.zipCode = zipCode;
    this.latitude = latitude;
    this.longitude = longitude;
    this.telephone = telephone;
    this.imageUrl = imageUrl;
    this.thumbnailUrl = thumbnailUrl;
    this.copyrightType = copyrightType;
    this.source = SOURCE_KTO_TOUR_API;
    this.legalDongRegionCode = legalDongRegionCode;
    this.legalDongSignguCode = legalDongSignguCode;
    this.classificationLevel1 = classificationLevel1;
    this.classificationLevel2 = classificationLevel2;
    this.classificationLevel3 = classificationLevel3;
    this.sourceCreatedAt = sourceCreatedAt;
    this.sourceModifiedAt = sourceModifiedAt;
    this.lastSyncedAt = lastSyncedAt;
  }

  public static Place create(String contentId, PlaceCategory category, String contentTypeId,
      String name, String address, String detailAddress, String zipCode, Double latitude,
      Double longitude, String telephone, String imageUrl, String thumbnailUrl,
      String copyrightType, String legalDongRegionCode, String legalDongSignguCode,
      String classificationLevel1, String classificationLevel2, String classificationLevel3,
      String sourceCreatedAt, String sourceModifiedAt, Instant lastSyncedAt) {
    return new Place(
        contentId, category, contentTypeId, name, address, detailAddress, zipCode, latitude,
        longitude, telephone, imageUrl, thumbnailUrl, copyrightType, legalDongRegionCode,
        legalDongSignguCode, classificationLevel1, classificationLevel2, classificationLevel3,
        sourceCreatedAt, sourceModifiedAt, lastSyncedAt);
  }

  public void updateFromSync(PlaceCategory category, String contentTypeId, String name,
      String address, String detailAddress, String zipCode, Double latitude, Double longitude,
      String telephone, String imageUrl, String thumbnailUrl, String copyrightType,
      String legalDongRegionCode, String legalDongSignguCode, String classificationLevel1,
      String classificationLevel2, String classificationLevel3, String sourceCreatedAt,
      String sourceModifiedAt, Instant syncedAt) {
    this.category = category;
    this.contentTypeId = contentTypeId;
    this.name = name;
    this.address = address;
    this.detailAddress = detailAddress;
    this.zipCode = zipCode;
    this.latitude = latitude;
    this.longitude = longitude;
    this.telephone = telephone;
    this.imageUrl = imageUrl;
    this.thumbnailUrl = thumbnailUrl;
    this.copyrightType = copyrightType;
    this.legalDongRegionCode = legalDongRegionCode;
    this.legalDongSignguCode = legalDongSignguCode;
    this.classificationLevel1 = classificationLevel1;
    this.classificationLevel2 = classificationLevel2;
    this.classificationLevel3 = classificationLevel3;
    this.sourceCreatedAt = sourceCreatedAt;
    this.sourceModifiedAt = sourceModifiedAt;
    this.lastSyncedAt = syncedAt;
  }

  public void updateTags(Set<PlaceTag> tags) {
    this.tags = new HashSet<>(tags);
  }
}
