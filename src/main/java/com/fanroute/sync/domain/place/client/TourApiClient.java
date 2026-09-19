package com.fanroute.sync.domain.place.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import com.fanroute.sync.domain.place.dto.TourApiDto;

/** TourAPI 4.4부터 폐지된 areaCode 대신 법정동 코드로 지역을 필터링합니다. */
public interface TourApiClient {

  @GetExchange("/searchStay2")
  TourApiDto.SearchStayResponse searchStay(
      @RequestParam("serviceKey") String serviceKey,
      @RequestParam("MobileOS") String mobileOs,
      @RequestParam("MobileApp") String mobileApp,
      @RequestParam("_type") String responseType,
      @RequestParam("arrange") String arrange,
      @RequestParam("lDongRegnCd") String legalDongRegionCode,
      @RequestParam("numOfRows") int numOfRows,
      @RequestParam("pageNo") int pageNo);

  @GetExchange("/areaBasedList2")
  TourApiDto.AreaBasedListResponse searchAreaBasedList(
      @RequestParam("serviceKey") String serviceKey,
      @RequestParam("MobileOS") String mobileOs,
      @RequestParam("MobileApp") String mobileApp,
      @RequestParam("_type") String responseType,
      @RequestParam("arrange") String arrange,
      @RequestParam("contentTypeId") String contentTypeId,
      @RequestParam("lDongRegnCd") String legalDongRegionCode,
      @RequestParam("numOfRows") int numOfRows,
      @RequestParam("pageNo") int pageNo);

  /** 좌표 기준 반경 검색. TourAPI가 거리(dist)를 직접 계산해 반환합니다. */
  @GetExchange("/locationBasedList2")
  TourApiDto.LocationBasedListResponse searchLocationBasedList(
      @RequestParam("serviceKey") String serviceKey,
      @RequestParam("MobileOS") String mobileOs,
      @RequestParam("MobileApp") String mobileApp,
      @RequestParam("_type") String responseType,
      @RequestParam("arrange") String arrange,
      @RequestParam("contentTypeId") String contentTypeId,
      @RequestParam("mapX") double longitude,
      @RequestParam("mapY") double latitude,
      @RequestParam("radius") int radiusMeters,
      @RequestParam("numOfRows") int numOfRows,
      @RequestParam("pageNo") int pageNo);
}
