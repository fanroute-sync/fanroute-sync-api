package com.fanroute.sync.domain.concert.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.entity.Genre;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.service.ConcertScheduleService;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.config.SecurityConfig;
import com.fanroute.sync.support.SecurityWebMvcTestSupport;

@WebMvcTest(ConcertScheduleAdminController.class)
@Import({SecurityConfig.class, SecurityWebMvcTestSupport.class})
class ConcertScheduleAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private ConcertScheduleService concertScheduleService;

  private static final String CREATE_BODY = """
      {"concertId": 1, "performanceDate": "2026-09-20", "performanceTime": "19:30"}
      """;

  private ConcertSchedule schedule() {
    Venue venue = Venue.create("FC001", "테스트홀", "부산", 35.1, 129.0);
    Concert concert = Concert.create(
        "PF001", venue, "테스트 공연", Genre.POPULAR_MUSIC, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 30), "poster.jpg", Instant.now());
    return ConcertSchedule.create(concert, 1, LocalDate.of(2026, 9, 20), LocalTime.of(19, 30));
  }

  @Test
  @DisplayName("ADMIN 권한을 가진 사용자가 회차를 생성할 수 있다")
  void createsScheduleForAdminUser() throws Exception {
    when(concertScheduleService.createSchedule(any())).thenReturn(schedule());

    mockMvc.perform(post("/api/v1/admin/concert-schedules")
            .with(jwt().jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(CREATE_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.round").value(1));
  }

  @Test
  @DisplayName("같은 날짜·시각이 중복되면 409를 반환한다")
  void returnsConflictWhenDateTimeDuplicated() throws Exception {
    when(concertScheduleService.createSchedule(any()))
        .thenThrow(new BusinessException(ConcertErrorCode.DUPLICATE_SCHEDULE_TIME));

    mockMvc.perform(post("/api/v1/admin/concert-schedules")
            .with(jwt().jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(CREATE_BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONCERT_DUPLICATE_SCHEDULE_TIME"));
  }

  @Test
  @DisplayName("ADMIN 권한을 가진 사용자가 회차를 수정할 수 있다")
  void updatesScheduleForAdminUser() throws Exception {
    when(concertScheduleService.updateSchedule(org.mockito.ArgumentMatchers.eq(1L), any()))
        .thenReturn(schedule());
    String body = """
        {"performanceDate": "2026-09-21", "performanceTime": "20:00"}
        """;

    mockMvc.perform(put("/api/v1/admin/concert-schedules/1")
            .with(jwt().jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("ADMIN 권한을 가진 사용자가 회차를 삭제할 수 있다")
  void deletesScheduleForAdminUser() throws Exception {
    mockMvc.perform(delete("/api/v1/admin/concert-schedules/1")
            .with(jwt().jwt(jwt -> jwt.subject("1"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("ADMIN 권한이 없는 사용자는 접근을 거부당한다")
  void rejectsNonAdminUser() throws Exception {
    mockMvc.perform(post("/api/v1/admin/concert-schedules")
            .with(jwt().jwt(jwt -> jwt.subject("1")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(CREATE_BODY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

    verifyNoInteractions(concertScheduleService);
  }

  @Test
  @DisplayName("인증되지 않은 요청은 거부한다")
  void rejectsUnauthenticatedRequest() throws Exception {
    mockMvc.perform(post("/api/v1/admin/concert-schedules")
            .contentType(MediaType.APPLICATION_JSON)
            .content(CREATE_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

    verifyNoInteractions(concertScheduleService);
  }
}
