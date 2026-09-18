package com.fanroute.sync.domain.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.service.PlaceAdminService;
import com.fanroute.sync.domain.place.service.PlaceService;
import com.fanroute.sync.global.config.SecurityConfig;
import com.fanroute.sync.global.batch.BatchJobLauncher;
import com.fanroute.sync.support.SecurityWebMvcTestSupport;

@WebMvcTest(PlaceAdminController.class)
@Import({SecurityConfig.class, SecurityWebMvcTestSupport.class})
class PlaceAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private BatchJobLauncher batchJobLauncher;
  @MockitoBean
  private PlaceAdminService placeAdminService;
  @MockitoBean
  private PlaceService placeService;
  @MockitoBean(name = "accommodationPlaceSyncJob")
  private Job accommodationPlaceSyncJob;
  @MockitoBean(name = "attractionPlaceSyncJob")
  private Job attractionPlaceSyncJob;
  @MockitoBean(name = "restaurantPlaceSyncJob")
  private Job restaurantPlaceSyncJob;

  private static RequestPostProcessor adminJwt() {
    return jwt().jwt(jwt -> jwt.subject("1")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  @Test
  @DisplayName("카테고리를 지정하면 해당 Job만 실행한다")
  void syncsSingleCategoryWhenSpecified() throws Exception {
    when(batchJobLauncher.start(eq(accommodationPlaceSyncJob), any(JobParameters.class)))
        .thenReturn(execution("accommodationPlaceSyncJob", 10, 9, 1));

    mockMvc.perform(post("/api/v1/admin/places/sync")
            .with(adminJwt())
            .param("category", "ACCOMMODATION"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].jobName").value("accommodationPlaceSyncJob"))
        .andExpect(jsonPath("$.data[0].readCount").value(10))
        .andExpect(jsonPath("$.data.length()").value(1));

    verify(batchJobLauncher, never()).start(eq(attractionPlaceSyncJob), any(JobParameters.class));
    verify(batchJobLauncher, never()).start(eq(restaurantPlaceSyncJob), any(JobParameters.class));
  }

  @Test
  @DisplayName("카테고리를 생략하면 세 Job을 모두 실행한다")
  void syncsAllCategoriesWhenNotSpecified() throws Exception {
    when(batchJobLauncher.start(eq(accommodationPlaceSyncJob), any(JobParameters.class)))
        .thenReturn(execution("accommodationPlaceSyncJob", 10, 10, 0));
    when(batchJobLauncher.start(eq(attractionPlaceSyncJob), any(JobParameters.class)))
        .thenReturn(execution("attractionPlaceSyncJob", 5, 5, 0));
    when(batchJobLauncher.start(eq(restaurantPlaceSyncJob), any(JobParameters.class)))
        .thenReturn(execution("restaurantPlaceSyncJob", 3, 3, 0));

    mockMvc.perform(post("/api/v1/admin/places/sync").with(adminJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].jobName").value("accommodationPlaceSyncJob"))
        .andExpect(jsonPath("$.data[1].jobName").value("attractionPlaceSyncJob"))
        .andExpect(jsonPath("$.data[2].jobName").value("restaurantPlaceSyncJob"));
  }

  @Test
  @DisplayName("전체 동기화 중 한 카테고리 Job이 실행 중이어도 나머지는 계속 진행하고 실패 사실을 응답에 남긴다")
  void isolatesLaunchFailureWhenSyncingAll() throws Exception {
    when(accommodationPlaceSyncJob.getName()).thenReturn("accommodationPlaceSyncJob");
    when(batchJobLauncher.start(eq(accommodationPlaceSyncJob), any(JobParameters.class)))
        .thenThrow(new JobExecutionAlreadyRunningException("running"));
    when(batchJobLauncher.start(eq(attractionPlaceSyncJob), any(JobParameters.class)))
        .thenReturn(execution("attractionPlaceSyncJob", 5, 5, 0));
    when(batchJobLauncher.start(eq(restaurantPlaceSyncJob), any(JobParameters.class)))
        .thenReturn(execution("restaurantPlaceSyncJob", 3, 3, 0));

    mockMvc.perform(post("/api/v1/admin/places/sync").with(adminJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(3))
        .andExpect(jsonPath("$.data[0].jobName").value("accommodationPlaceSyncJob"))
        .andExpect(jsonPath("$.data[0].status").value("LAUNCH_FAILED"))
        .andExpect(jsonPath("$.data[0].exitCode").value("PLACE_SYNC_ALREADY_RUNNING"))
        .andExpect(jsonPath("$.data[1].jobName").value("attractionPlaceSyncJob"))
        .andExpect(jsonPath("$.data[2].jobName").value("restaurantPlaceSyncJob"));
  }

  @Test
  @DisplayName("단일 카테고리 요청에서 Job이 이미 실행 중이면 409를 반환한다")
  void returnsConflictForSingleCategoryAlreadyRunning() throws Exception {
    when(batchJobLauncher.start(eq(accommodationPlaceSyncJob), any(JobParameters.class)))
        .thenThrow(new JobExecutionAlreadyRunningException("running"));

    mockMvc.perform(post("/api/v1/admin/places/sync")
            .with(adminJwt())
            .param("category", "ACCOMMODATION"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PLACE_SYNC_ALREADY_RUNNING"));
  }

  @Test
  @DisplayName("ADMIN 권한이 없는 사용자는 접근을 거부당한다")
  void rejectsNonAdminUser() throws Exception {
    mockMvc.perform(post("/api/v1/admin/places/sync").with(jwt().jwt(jwt -> jwt.subject("1"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

    verifyNoInteractions(batchJobLauncher);
  }

  @Test
  @DisplayName("인증되지 않은 요청은 거부한다")
  void rejectsUnauthenticatedRequest() throws Exception {
    mockMvc.perform(post("/api/v1/admin/places/sync"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

    verifyNoInteractions(batchJobLauncher);
  }

  @Test
  @DisplayName("좌표 주변 인기 장소 후보를 조회한다")
  void getsNearbyCandidates() throws Exception {
    when(placeAdminService.searchNearbyCandidates(1L, PlaceCategory.RESTAURANT, 1000))
        .thenReturn(List.of(placeSummary()));
    Place existing = Place.create(
        "2868824", PlaceCategory.RESTAURANT, "39", "비스포레", "부산", null, null, 35.1636, 129.1287,
        null, null, null, null, "26", null, null, null, null, null, null,
        java.time.Instant.now());
    org.springframework.test.util.ReflectionTestUtils.setField(existing, "id", 7L);
    when(placeAdminService.findExistingByContentId("2868824")).thenReturn(existing);

    mockMvc.perform(get("/api/v1/admin/places/nearby-candidates")
            .with(adminJwt())
            .param("category", "RESTAURANT")
            .param("venueId", "1")
            .param("radiusMeters", "1000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].contentId").value("2868824"))
        .andExpect(jsonPath("$.data[0].name").value("비스포레"))
        .andExpect(jsonPath("$.data[0].existingPlaceId").value(7));
  }

  @Test
  @DisplayName("ADMIN 권한이 없는 사용자는 인기 장소 후보 조회를 거부당한다")
  void rejectsNonAdminUserForNearbyCandidates() throws Exception {
    mockMvc.perform(get("/api/v1/admin/places/nearby-candidates")
            .with(jwt().jwt(jwt -> jwt.subject("1")))
            .param("category", "RESTAURANT")
            .param("latitude", "35.1691")
            .param("longitude", "129.1364"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(placeAdminService);
  }

  private TourApiDto.PlaceSummary placeSummary() {
    return new TourApiDto.PlaceSummary(
        "2868824", "39", "비스포레", "부산광역시 수영구", null, null, 129.1287, 35.1637, null, null,
        null, null, "26", null, null, null, null, null, null, 910.87);
  }

  private JobExecution execution(String jobName, int read, int write, int skip) {
    JobInstance jobInstance = new JobInstance(1L, jobName);
    JobExecution execution = new JobExecution(1L, jobInstance, new JobParameters());
    execution.setStatus(BatchStatus.COMPLETED);
    execution.setExitStatus(ExitStatus.COMPLETED);
    execution.setStartTime(LocalDateTime.now());
    execution.setEndTime(LocalDateTime.now());

    StepExecution stepExecution = new StepExecution(1L, jobName + "Step", execution);
    stepExecution.setReadCount(read);
    stepExecution.setWriteCount(write);
    stepExecution.setWriteSkipCount(skip);
    execution.addStepExecution(stepExecution);

    return execution;
  }
}
