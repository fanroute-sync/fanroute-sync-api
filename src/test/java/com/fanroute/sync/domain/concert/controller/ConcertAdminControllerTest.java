package com.fanroute.sync.domain.concert.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fanroute.sync.domain.concert.service.ConcertSyncService;
import com.fanroute.sync.global.config.AdminConfig;
import com.fanroute.sync.global.config.SecurityConfig;

@WebMvcTest(ConcertAdminController.class)
@Import({SecurityConfig.class, AdminConfig.class})
@TestPropertySource(properties = "admin.api-key=test-admin-key")
class ConcertAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private ConcertSyncService concertSyncService;
  @MockitoBean(name = "jwtDecoder")
  private JwtDecoder jwtDecoder;

  @Test
  @DisplayName("올바른 관리자 키로 요청하면 동기화를 실행한다")
  void syncsWithValidAdminKey() throws Exception {
    when(concertSyncService.syncConcerts())
        .thenReturn(new ConcertSyncService.SyncResult(10, 9, 1));

    mockMvc.perform(post("/api/v1/admin/concerts/sync").header("X-Admin-Key", "test-admin-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(10))
        .andExpect(jsonPath("$.data.succeeded").value(9))
        .andExpect(jsonPath("$.data.failed").value(1));
  }

  @Test
  @DisplayName("관리자 키 헤더가 없으면 거부한다")
  void rejectsMissingAdminKey() throws Exception {
    mockMvc.perform(post("/api/v1/admin/concerts/sync"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONCERT_ADMIN_ACCESS_DENIED"));

    verifyNoInteractions(concertSyncService);
  }

  @Test
  @DisplayName("관리자 키가 틀리면 거부한다")
  void rejectsWrongAdminKey() throws Exception {
    mockMvc.perform(post("/api/v1/admin/concerts/sync").header("X-Admin-Key", "wrong-key"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONCERT_ADMIN_ACCESS_DENIED"));

    verifyNoInteractions(concertSyncService);
  }
}
