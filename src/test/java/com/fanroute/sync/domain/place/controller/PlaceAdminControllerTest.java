package com.fanroute.sync.domain.place.controller;

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

import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.service.PlaceSyncService;
import com.fanroute.sync.global.config.AdminConfig;
import com.fanroute.sync.global.config.SecurityConfig;

@WebMvcTest(PlaceAdminController.class)
@Import({SecurityConfig.class, AdminConfig.class})
@TestPropertySource(properties = "admin.api-key=test-admin-key")
class PlaceAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private PlaceSyncService placeSyncService;
  @MockitoBean(name = "jwtDecoder")
  private JwtDecoder jwtDecoder;

  @Test
  @DisplayName("올바른 관리자 키로 요청하면 동기화를 실행한다")
  void syncsWithValidAdminKey() throws Exception {
    when(placeSyncService.sync(PlaceCategory.ACCOMMODATION))
        .thenReturn(new PlaceSyncService.SyncResult(PlaceCategory.ACCOMMODATION, 42));

    mockMvc.perform(post("/api/v1/admin/places/sync").header("X-Admin-Key", "test-admin-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.category").value("ACCOMMODATION"))
        .andExpect(jsonPath("$.data.upsertedCount").value(42));
  }

  @Test
  @DisplayName("관리자 키 헤더가 없으면 거부한다")
  void rejectsMissingAdminKey() throws Exception {
    mockMvc.perform(post("/api/v1/admin/places/sync"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PLACE_ADMIN_ACCESS_DENIED"));

    verifyNoInteractions(placeSyncService);
  }

  @Test
  @DisplayName("관리자 키가 틀리면 거부한다")
  void rejectsWrongAdminKey() throws Exception {
    mockMvc.perform(post("/api/v1/admin/places/sync").header("X-Admin-Key", "wrong-key"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PLACE_ADMIN_ACCESS_DENIED"));

    verifyNoInteractions(placeSyncService);
  }
}
