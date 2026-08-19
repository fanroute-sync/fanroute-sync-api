package com.fanroute.sync.domain.concert.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.service.ConcertService;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.config.SecurityConfig;
import com.fanroute.sync.global.config.WebMvcConfig;

@WebMvcTest(ConcertController.class)
@Import({SecurityConfig.class, WebMvcConfig.class})
class ConcertControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private ConcertService concertService;
  @MockitoBean(name = "jwtDecoder")
  private JwtDecoder jwtDecoder;

  @Test
  @DisplayName("인증 없이 공연 목록을 조회할 수 있다")
  void getsConcertsWithoutAuthentication() throws Exception {
    Venue venue = Venue.create("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0);
    Concert concert = Concert.create(
        "PF001", venue, "테스트 공연", Genre.POPULAR_MUSIC, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 2), "poster.jpg", Instant.now());
    Page<Concert> page = new PageImpl<>(List.of(concert));
    when(concertService.getConcerts(isNull(), any())).thenReturn(page);

    mockMvc.perform(get("/api/v1/concerts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].title").value("테스트 공연"))
        .andExpect(jsonPath("$.data.content[0].venue.name").value("테스트홀"));
  }

  @Test
  @DisplayName("인증 없이 공연 상세를 조회할 수 있다")
  void getsConcertDetailWithoutAuthentication() throws Exception {
    Venue venue = Venue.create("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0);
    Concert concert = Concert.create(
        "PF001", venue, "테스트 공연", Genre.POPULAR_MUSIC, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 2), "poster.jpg", Instant.now());
    when(concertService.getConcert(1L)).thenReturn(concert);

    mockMvc.perform(get("/api/v1/concerts/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("테스트 공연"));
  }

  @Test
  @DisplayName("장르가 없는 공연 상세 조회도 정상 응답한다")
  void getsConcertDetailWithNullGenre() throws Exception {
    Venue venue = Venue.create("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0);
    Concert concert = Concert.create(
        "PF001", venue, "테스트 공연", null, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 2), "poster.jpg", Instant.now());
    when(concertService.getConcert(1L)).thenReturn(concert);

    mockMvc.perform(get("/api/v1/concerts/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.genreName").doesNotExist());
  }

  @Test
  @DisplayName("브라우저의 Accept 헤더로 요청해도 XML이 아닌 JSON으로 응답한다")
  void respondsWithJsonEvenWhenBrowserPrefersXml() throws Exception {
    Venue venue = Venue.create("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0);
    Concert concert = Concert.create(
        "PF001", venue, "테스트 공연", Genre.POPULAR_MUSIC, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 2), "poster.jpg", Instant.now());
    when(concertService.getConcert(1L)).thenReturn(concert);

    mockMvc.perform(get("/api/v1/concerts/1")
            .header("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.data.title").value("테스트 공연"));
  }

  @Test
  @DisplayName("존재하지 않는 장르로 필터링하면 400을 반환한다")
  void returnsBadRequestForInvalidGenre() throws Exception {
    mockMvc.perform(get("/api/v1/concerts").param("genreName", "없는장르"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CONCERT_INVALID_GENRE"));
  }

  @Test
  @DisplayName("존재하지 않는 공연을 조회하면 404를 반환한다")
  void returnsNotFoundForMissingConcert() throws Exception {
    when(concertService.getConcert(1L))
        .thenThrow(new BusinessException(ConcertErrorCode.CONCERT_NOT_FOUND));

    mockMvc.perform(get("/api/v1/concerts/1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CONCERT_NOT_FOUND"));
  }
}
