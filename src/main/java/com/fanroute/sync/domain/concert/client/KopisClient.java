package com.fanroute.sync.domain.concert.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import com.fanroute.sync.domain.concert.dto.KopisDto;

public interface KopisClient {

  @GetExchange("/pblprfr")
  KopisDto.PerformanceListResponse getPerformances(
      @RequestParam("service") String serviceKey,
      @RequestParam("stdate") String startDate,
      @RequestParam("eddate") String endDate,
      @RequestParam("cpage") int page,
      @RequestParam("rows") int rows,
      @RequestParam(value = "signgucode", required = false) String regionCode);

  @GetExchange("/pblprfr/{mt20id}")
  KopisDto.PerformanceDetailResponse getPerformanceDetail(
      @PathVariable("mt20id") String kopisConcertId,
      @RequestParam("service") String serviceKey);

  @GetExchange("/prfplc/{mt10id}")
  KopisDto.VenueDetailResponse getVenueDetail(
      @PathVariable("mt10id") String kopisVenueId,
      @RequestParam("service") String serviceKey);
}
