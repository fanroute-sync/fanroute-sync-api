package com.fanroute.sync.domain.place.entity;

import java.util.Arrays;

/** TourAPI contentTypeId와 서비스 카테고리의 매핑을 관리합니다. */
public enum PlaceCategory {

  ACCOMMODATION("32"),
  ATTRACTION("12"),
  CULTURAL_FACILITY("14"),
  SHOPPING("38"),
  RESTAURANT("39");

  private final String tourApiContentTypeId;

  PlaceCategory(String tourApiContentTypeId) {
    this.tourApiContentTypeId = tourApiContentTypeId;
  }

  public String tourApiContentTypeId() {
    return tourApiContentTypeId;
  }

  public static PlaceCategory fromContentTypeId(String contentTypeId) {
    return Arrays.stream(values())
        .filter(category -> category.tourApiContentTypeId.equals(contentTypeId))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("알 수 없는 contentTypeId: " + contentTypeId));
  }
}
