package com.fanroute.sync.domain.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.domain.place.service.PlaceService;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.config.SecurityConfig;
import com.fanroute.sync.global.config.WebMvcConfig;

@WebMvcTest(PlaceController.class)
@Import({SecurityConfig.class, WebMvcConfig.class})
class PlaceControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private PlaceService placeService;
  @MockitoBean(name = "jwtDecoder")
  private JwtDecoder jwtDecoder;

  @Test
  @DisplayName("인증 없이 장소 목록을 조회할 수 있다")
  void getsPlacesWithoutAuthentication() throws Exception {
    Place place = testPlace();
    Page<Place> page = new PageImpl<>(List.of(place));
    when(placeService.getPlaces(isNull(), any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/places"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].name").value("테스트 호텔"));
  }

  @Test
  @DisplayName("인증 없이 장소 상세를 조회할 수 있다")
  void getsPlaceDetailWithoutAuthentication() throws Exception {
    when(placeService.getPlace(1L)).thenReturn(testPlace());

    mockMvc.perform(get("/api/v1/places/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("테스트 호텔"));
  }

  @Test
  @DisplayName("응답에 이미지 저작권 유형과 출처가 함께 노출된다")
  void exposesCopyrightTypeAndSourceForAttribution() throws Exception {
    when(placeService.getPlace(1L)).thenReturn(testPlace());

    mockMvc.perform(get("/api/v1/places/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.copyrightType").value("Type3"))
        .andExpect(jsonPath("$.data.source").value("KTO_TOUR_API"));
  }

  @Test
  @DisplayName("잘못된 카테고리로 조회하면 400을 반환한다")
  void returnsBadRequestForInvalidCategory() throws Exception {
    mockMvc.perform(get("/api/v1/places").param("category", "없는카테고리"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("존재하지 않는 장소를 조회하면 404를 반환한다")
  void returnsNotFoundForMissingPlace() throws Exception {
    when(placeService.getPlace(1L))
        .thenThrow(new BusinessException(PlaceErrorCode.PLACE_NOT_FOUND));

    mockMvc.perform(get("/api/v1/places/1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
  }

  private Place testPlace() {
    return Place.create(
        "126508", PlaceCategory.ACCOMMODATION, "32", "테스트 호텔", "부산 해운대구", null, null, 35.1,
        129.0, null, "image.jpg", null, "Type3", "26", null, null, null, null, null, null,
        Instant.now());
  }
}
