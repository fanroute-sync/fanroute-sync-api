package com.fanroute.sync.domain.place.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

/** TourAPI 활용매뉴얼 4.4의 JSON 응답 모델입니다. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TourApiDto {

  public record SearchStayResponse(@JsonProperty("response") Response response) {

    public List<PlaceSummary> itemsOrEmpty() {
      if (response == null || response.body() == null || response.body().items() == null
          || response.body().items().item() == null) {
        return List.of();
      }
      return response.body().items().item();
    }

    public int totalCount() {
      return response == null || response.body() == null ? 0 : response.body().totalCount();
    }
  }

  public record Response(
      @JsonProperty("header") Header header,
      @JsonProperty("body") Body body) {

  }

  public record Header(
      @JsonProperty("resultCode") String resultCode,
      @JsonProperty("resultMsg") String resultMsg) {

  }

  public record Body(
      // 결과가 없으면 items가 객체 대신 빈 문자열로 내려옵니다.
      @JsonProperty("items") @JsonDeserialize(using = ItemsDeserializer.class) Items items,
      @JsonProperty("numOfRows") int numOfRows,
      @JsonProperty("pageNo") int pageNo,
      @JsonProperty("totalCount") int totalCount) {

  }

  public record Items(@JsonProperty("item") List<PlaceSummary> item) {

  }

  static final class ItemsDeserializer extends ValueDeserializer<Items> {

    @Override
    public Items deserialize(JsonParser parser, DeserializationContext context) {
      if (parser.currentToken() == JsonToken.VALUE_STRING) {
        parser.getString();
        return new Items(List.of());
      }
      return context.readValue(parser, Items.class);
    }
  }

  public record PlaceSummary(
      @JsonProperty("contentid") String contentId,
      @JsonProperty("contenttypeid") String contentTypeId,
      @JsonProperty("title") String name,
      @JsonProperty("addr1") String address,
      @JsonProperty("addr2") String detailAddress,
      @JsonProperty("zipcode") String zipCode,
      // TourAPI의 mapx는 경도, mapy는 위도입니다.
      @JsonProperty("mapx") Double longitude,
      @JsonProperty("mapy") Double latitude,
      @JsonProperty("tel") String telephone,
      @JsonProperty("firstimage") String imageUrl,
      @JsonProperty("firstimage2") String thumbnailUrl,
      @JsonProperty("cpyrhtDivCd") String copyrightType,
      @JsonProperty("lDongRegnCd") String legalDongRegionCode,
      @JsonProperty("lDongSignguCd") String legalDongSignguCode,
      @JsonProperty("lclsSystm1") String classificationLevel1,
      @JsonProperty("lclsSystm2") String classificationLevel2,
      @JsonProperty("lclsSystm3") String classificationLevel3,
      @JsonProperty("createdtime") String sourceCreatedAt,
      @JsonProperty("modifiedtime") String sourceModifiedAt) {

  }
}
