package com.fanroute.sync.domain.place.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
  @DisplayName("카테고리를 지정하면 해당 카테고리만 동기화한다")
  void syncsSingleCategoryWhenSpecified() throws Exception {
    when(placeSyncService.sync(PlaceCategory.ACCOMMODATION))
        .thenReturn(new PlaceSyncService.SyncResult(PlaceCategory.ACCOMMODATION, 42));

    mockMvc.perform(post("/api/v1/admin/places/sync")
            .header("X-Admin-Key", "test-admin-key")
            .param("category", "ACCOMMODATION"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].category").value("ACCOMMODATION"))
        .andExpect(jsonPath("$.data[0].success").value(true))
        .andExpect(jsonPath("$.data[0].upsertedCount").value(42));

    verify(placeSyncService, never()).syncAll();
  }

  @Test
  @DisplayName("카테고리를 생략하면 전체 카테고리를 동기화한다")
  void syncsAllCategoriesWhenNotSpecified() throws Exception {
    when(placeSyncService.syncAll()).thenReturn(List.of(
        new PlaceSyncService.SyncOutcome(PlaceCategory.ACCOMMODATION, true, 10, null),
        new PlaceSyncService.SyncOutcome(PlaceCategory.ATTRACTION, false, 0, "server error"),
        new PlaceSyncService.SyncOutcome(PlaceCategory.RESTAURANT, true, 5, null)));

    mockMvc.perform(post("/api/v1/admin/places/sync").header("X-Admin-Key", "test-admin-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].category").value("ACCOMMODATION"))
        .andExpect(jsonPath("$.data[0].success").value(true))
        .andExpect(jsonPath("$.data[1].category").value("ATTRACTION"))
        .andExpect(jsonPath("$.data[1].success").value(false))
        .andExpect(jsonPath("$.data[1].failureMessage").value("server error"))
        .andExpect(jsonPath("$.data[2].category").value("RESTAURANT"));
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
