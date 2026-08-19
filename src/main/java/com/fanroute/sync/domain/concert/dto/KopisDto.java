package com.fanroute.sync.domain.concert.dto;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.List;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KopisDto {

  @JsonRootName("dbs")
  public record PerformanceListResponse(
      @JacksonXmlProperty(localName = "db")
      @JacksonXmlElementWrapper(useWrapping = false)
      List<PerformanceSummary> performances) {

    public List<PerformanceSummary> performancesOrEmpty() {
      return performances == null ? List.of() : performances;
    }
  }

  public record PerformanceSummary(
      @JacksonXmlProperty(localName = "mt20id") String kopisConcertId,
      @JacksonXmlProperty(localName = "prfnm") String title,
      @JacksonXmlProperty(localName = "prfpdfrom") String startDate,
      @JacksonXmlProperty(localName = "prfpdto") String endDate,
      @JacksonXmlProperty(localName = "fcltynm") String venueName,
      @JacksonXmlProperty(localName = "poster") String posterUrl,
      @JacksonXmlProperty(localName = "genrenm") String genreName) {

  }

  @JsonRootName("dbs")
  public record PerformanceDetailResponse(
      @JacksonXmlProperty(localName = "db") PerformanceDetail performance) {

  }

  public record PerformanceDetail(
      @JacksonXmlProperty(localName = "mt20id") String kopisConcertId,
      @JacksonXmlProperty(localName = "mt10id") String kopisVenueId,
      @JacksonXmlProperty(localName = "prfnm") String title,
      @JacksonXmlProperty(localName = "prfpdfrom") String startDate,
      @JacksonXmlProperty(localName = "prfpdto") String endDate,
      @JacksonXmlProperty(localName = "fcltynm") String venueName,
      @JacksonXmlProperty(localName = "poster") String posterUrl,
      @JacksonXmlProperty(localName = "genrenm") String genreName,
      @JacksonXmlProperty(localName = "prfstate") String status) {

  }

  @JsonRootName("dbs")
  public record VenueDetailResponse(
      @JacksonXmlProperty(localName = "db") VenueDetail venue) {

  }

  public record VenueDetail(
      @JacksonXmlProperty(localName = "mt10id") String kopisVenueId,
      @JacksonXmlProperty(localName = "fcltynm") String name,
      @JacksonXmlProperty(localName = "adres") String address,
      @JacksonXmlProperty(localName = "la") Double latitude,
      @JacksonXmlProperty(localName = "lo") Double longitude) {

  }
}
