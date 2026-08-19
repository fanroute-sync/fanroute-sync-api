package com.fanroute.sync.domain.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fanroute.sync.global.config.AdminConfig;
import com.fanroute.sync.global.config.SecurityConfig;

@WebMvcTest(PlaceAdminController.class)
@Import({SecurityConfig.class, AdminConfig.class})
@TestPropertySource(properties = "admin.api-key=test-admin-key")
class PlaceAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private JobOperator jobOperator;
  @MockitoBean(name = "accommodationPlaceSyncJob")
  private Job accommodationPlaceSyncJob;
  @MockitoBean(name = "attractionPlaceSyncJob")
  private Job attractionPlaceSyncJob;
  @MockitoBean(name = "restaurantPlaceSyncJob")
  private Job restaurantPlaceSyncJob;
  @MockitoBean(name = "jwtDecoder")
  private JwtDecoder jwtDecoder;

  @Test
  @DisplayName("카테고리를 지정하면 해당 Job만 실행한다")
  void syncsSingleCategoryWhenSpecified() throws Exception {
    when(jobOperator.start(eq(accommodationPlaceSyncJob), any(JobParameters.class)))
        .thenReturn(execution("accommodationPlaceSyncJob", 10, 9, 1));

    mockMvc.perform(post("/api/v1/admin/places/sync")
            .header("X-Admin-Key", "test-admin-key")
            .param("category", "ACCOMMODATION"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].jobName").value("accommodationPlaceSyncJob"))
        .andExpect(jsonPath("$.data[0].readCount").value(10))
        .andExpect(jsonPath("$.data.length()").value(1));

    verify(jobOperator, never()).start(eq(attractionPlaceSyncJob), any(JobParameters.class));
    verify(jobOperator, never()).start(eq(restaurantPlaceSyncJob), any(JobParameters.class));
  }

  @Test
  @DisplayName("카테고리를 생략하면 세 Job을 모두 실행한다")
  void syncsAllCategoriesWhenNotSpecified() throws Exception {
    when(jobOperator.start(eq(accommodationPlaceSyncJob), any(JobParameters.class)))
        .thenReturn(execution("accommodationPlaceSyncJob", 10, 10, 0));
    when(jobOperator.start(eq(attractionPlaceSyncJob), any(JobParameters.class)))
        .thenReturn(execution("attractionPlaceSyncJob", 5, 5, 0));
    when(jobOperator.start(eq(restaurantPlaceSyncJob), any(JobParameters.class)))
        .thenReturn(execution("restaurantPlaceSyncJob", 3, 3, 0));

    mockMvc.perform(post("/api/v1/admin/places/sync").header("X-Admin-Key", "test-admin-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].jobName").value("accommodationPlaceSyncJob"))
        .andExpect(jsonPath("$.data[1].jobName").value("attractionPlaceSyncJob"))
        .andExpect(jsonPath("$.data[2].jobName").value("restaurantPlaceSyncJob"));
  }

  @Test
  @DisplayName("전체 동기화 중 한 카테고리 Job이 실행 중이어도 나머지는 계속 진행한다")
  void isolatesLaunchFailureWhenSyncingAll() throws Exception {
    when(jobOperator.start(eq(accommodationPlaceSyncJob), any(JobParameters.class)))
        .thenThrow(new JobExecutionAlreadyRunningException("running"));
    when(jobOperator.start(eq(attractionPlaceSyncJob), any(JobParameters.class)))
        .thenReturn(execution("attractionPlaceSyncJob", 5, 5, 0));
    when(jobOperator.start(eq(restaurantPlaceSyncJob), any(JobParameters.class)))
        .thenReturn(execution("restaurantPlaceSyncJob", 3, 3, 0));

    mockMvc.perform(post("/api/v1/admin/places/sync").header("X-Admin-Key", "test-admin-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].jobName").value("attractionPlaceSyncJob"))
        .andExpect(jsonPath("$.data[1].jobName").value("restaurantPlaceSyncJob"));
  }

  @Test
  @DisplayName("단일 카테고리 요청에서 Job이 이미 실행 중이면 409를 반환한다")
  void returnsConflictForSingleCategoryAlreadyRunning() throws Exception {
    when(jobOperator.start(eq(accommodationPlaceSyncJob), any(JobParameters.class)))
        .thenThrow(new JobExecutionAlreadyRunningException("running"));

    mockMvc.perform(post("/api/v1/admin/places/sync")
            .header("X-Admin-Key", "test-admin-key")
            .param("category", "ACCOMMODATION"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PLACE_SYNC_ALREADY_RUNNING"));
  }

  @Test
  @DisplayName("관리자 키 헤더가 없으면 거부한다")
  void rejectsMissingAdminKey() throws Exception {
    mockMvc.perform(post("/api/v1/admin/places/sync"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PLACE_ADMIN_ACCESS_DENIED"));

    verifyNoInteractions(jobOperator);
  }

  @Test
  @DisplayName("관리자 키가 틀리면 거부한다")
  void rejectsWrongAdminKey() throws Exception {
    mockMvc.perform(post("/api/v1/admin/places/sync").header("X-Admin-Key", "wrong-key"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PLACE_ADMIN_ACCESS_DENIED"));

    verifyNoInteractions(jobOperator);
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
