package com.fanroute.sync.domain.place.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class TourApiDtoJsonMappingTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Test
  @DisplayName("숙박정보조회 JSON 응답을 올바르게 역직렬화한다")
  void parsesSearchStayJson() {
    String json = """
        {
          "response": {
            "header": { "resultCode": "0000", "resultMsg": "OK" },
            "body": {
              "items": {
                "item": [
                  {
                    "contentid": "126508",
                    "contenttypeid": "32",
                    "title": "테스트 호텔",
                    "addr1": "부산 해운대구",
                    "addr2": "101호",
                    "zipcode": "48058",
                    "mapx": "129.163",
                    "mapy": "35.163",
                    "tel": "051-000-0000",
                    "firstimage": "http://example.com/image.jpg",
                    "firstimage2": "http://example.com/thumb.jpg",
                    "cpyrhtDivCd": "Type3",
                    "lDongRegnCd": "26",
                    "lDongSignguCd": "26350",
                    "lclsSystm1": "AC",
                    "lclsSystm2": "AC01",
                    "lclsSystm3": "AC01010100",
                    "createdtime": "20260101120000",
                    "modifiedtime": "20260102120000"
                  }
                ]
              },
              "numOfRows": 50,
              "pageNo": 1,
              "totalCount": 1
            }
          }
        }
        """;

    TourApiDto.SearchStayResponse response =
        jsonMapper.readValue(json, TourApiDto.SearchStayResponse.class);

    assertThat(response.totalCount()).isEqualTo(1);
    assertThat(response.itemsOrEmpty()).hasSize(1);
    TourApiDto.PlaceSummary summary = response.itemsOrEmpty().get(0);
    assertThat(summary.contentId()).isEqualTo("126508");
    assertThat(summary.contentTypeId()).isEqualTo("32");
    assertThat(summary.name()).isEqualTo("테스트 호텔");
    assertThat(summary.address()).isEqualTo("부산 해운대구");
    assertThat(summary.zipCode()).isEqualTo("48058");
    assertThat(summary.longitude()).isEqualTo(129.163);
    assertThat(summary.latitude()).isEqualTo(35.163);
    assertThat(summary.imageUrl()).isEqualTo("http://example.com/image.jpg");
    assertThat(summary.telephone()).isEqualTo("051-000-0000");
    assertThat(summary.legalDongRegionCode()).isEqualTo("26");
    assertThat(summary.legalDongSignguCode()).isEqualTo("26350");
    assertThat(summary.classificationLevel1()).isEqualTo("AC");
    assertThat(summary.sourceCreatedAt()).isEqualTo("20260101120000");
    assertThat(summary.sourceModifiedAt()).isEqualTo("20260102120000");
  }

  @Test
  @DisplayName("item이 없는 응답은 빈 목록으로 처리한다")
  void handlesEmptyItems() {
    String json = """
        {
          "response": {
            "header": { "resultCode": "0000", "resultMsg": "OK" },
            "body": { "items": "", "numOfRows": 50, "pageNo": 1, "totalCount": 0 }
          }
        }
        """;

    TourApiDto.SearchStayResponse response =
        jsonMapper.readValue(json, TourApiDto.SearchStayResponse.class);

    assertThat(response.itemsOrEmpty()).isEmpty();
  }
}
