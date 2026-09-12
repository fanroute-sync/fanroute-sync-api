package com.fanroute.sync.domain.concert.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.service.ConcertScheduleService;
import com.fanroute.sync.global.config.SecurityConfig;
import com.fanroute.sync.global.config.WebMvcConfig;
import com.fanroute.sync.support.SecurityWebMvcTestSupport;

@WebMvcTest(ConcertScheduleController.class)
@Import({SecurityConfig.class, WebMvcConfig.class, SecurityWebMvcTestSupport.class})
class ConcertScheduleControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private ConcertScheduleService concertScheduleService;

  @Test
  @DisplayName("인증 없이 공연별 회차 목록을 조회할 수 있다")
  void getsSchedulesWithoutAuthentication() throws Exception {
    Venue venue = Venue.create("FC001", "테스트홀", "부산", 35.1, 129.0);
    Concert concert = Concert.create(
        "PF001", venue, "테스트 공연", Genre.POPULAR_MUSIC, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 30), "poster.jpg", Instant.now());
    ConcertSchedule schedule =
        ConcertSchedule.create(concert, 1, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));
    when(concertScheduleService.getSchedulesByConcert(eq(1L))).thenReturn(List.of(schedule));

    mockMvc.perform(get("/api/v1/concert-schedules").param("concertId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].round").value(1))
        .andExpect(jsonPath("$.data[0].performanceDate").value("2026-09-20"))
        .andExpect(jsonPath("$.data[0].performanceTime").value("19:30:00"))
        .andExpect(jsonPath("$.data[0].provisional").value(false))
        .andExpect(jsonPath("$.data[0].source").value("MANUAL"));
  }
}
