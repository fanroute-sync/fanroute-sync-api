package com.fanroute.sync.domain.concert.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.xml.XmlMapper;

class KopisDtoXmlMappingTest {

  private final XmlMapper xmlMapper = XmlMapper.builder().build();

  @Test
  @DisplayName("공연목록조회 XML 응답을 올바르게 역직렬화한다")
  void parsesPerformanceListXml() {
    String xml = """
        <dbs>
          <db>
            <mt20id>PF229754</mt20id>
            <prfnm>테스트 공연</prfnm>
            <prfpdfrom>2026.09.01</prfpdfrom>
            <prfpdto>2026.09.02</prfpdto>
            <fcltynm>테스트홀</fcltynm>
            <poster>http://example.com/poster.jpg</poster>
            <genrenm>대중음악</genrenm>
          </db>
        </dbs>
        """;

    KopisDto.PerformanceListResponse response =
        xmlMapper.readValue(xml, KopisDto.PerformanceListResponse.class);

    assertThat(response.performancesOrEmpty()).hasSize(1);
    KopisDto.PerformanceSummary summary = response.performancesOrEmpty().get(0);
    assertThat(summary.kopisConcertId()).isEqualTo("PF229754");
    assertThat(summary.title()).isEqualTo("테스트 공연");
    assertThat(summary.venueName()).isEqualTo("테스트홀");
    assertThat(summary.genreName()).isEqualTo("대중음악");
  }

  @Test
  @DisplayName("공연 상세조회 XML 응답을 올바르게 역직렬화한다")
  void parsesPerformanceDetailXml() {
    String xml = """
        <dbs>
          <db>
            <mt20id>PF229754</mt20id>
            <mt10id>FC000001</mt10id>
            <prfnm>테스트 공연</prfnm>
            <prfpdfrom>2026.09.01</prfpdfrom>
            <prfpdto>2026.09.02</prfpdto>
            <fcltynm>테스트홀</fcltynm>
            <poster>http://example.com/poster.jpg</poster>
            <genrenm>대중음악</genrenm>
            <prfstate>공연예정</prfstate>
          </db>
        </dbs>
        """;

    KopisDto.PerformanceDetailResponse response =
        xmlMapper.readValue(xml, KopisDto.PerformanceDetailResponse.class);

    assertThat(response.performance()).isNotNull();
    assertThat(response.performance().kopisConcertId()).isEqualTo("PF229754");
    assertThat(response.performance().kopisVenueId()).isEqualTo("FC000001");
    assertThat(response.performance().status()).isEqualTo("공연예정");
  }

  @Test
  @DisplayName("공연시설 상세조회 XML 응답을 올바르게 역직렬화한다")
  void parsesVenueDetailXml() {
    String xml = """
        <dbs>
          <db>
            <mt10id>FC000001</mt10id>
            <fcltynm>테스트홀</fcltynm>
            <adres>부산 해운대구</adres>
            <la>35.1</la>
            <lo>129.0</lo>
          </db>
        </dbs>
        """;

    KopisDto.VenueDetailResponse response =
        xmlMapper.readValue(xml, KopisDto.VenueDetailResponse.class);

    assertThat(response.venue()).isNotNull();
    assertThat(response.venue().kopisVenueId()).isEqualTo("FC000001");
    assertThat(response.venue().name()).isEqualTo("테스트홀");
    assertThat(response.venue().latitude()).isEqualTo(35.1);
    assertThat(response.venue().longitude()).isEqualTo(129.0);
  }
}
