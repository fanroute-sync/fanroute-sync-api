package com.fanroute.sync.domain.concert.controller;

import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(ConcertAdminController.class)
@Import({SecurityConfig.class, AdminConfig.class})
@TestPropertySource(properties = "admin.api-key=test-admin-key")
class ConcertAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private JobOperator jobOperator;
  @MockitoBean
  private Job kopisConcertSyncJob;
  @MockitoBean(name = "jwtDecoder")
  private JwtDecoder jwtDecoder;

  @Test
  @DisplayName("올바른 관리자 키로 요청하면 Job을 실행하고 실행 결과를 반환한다")
  void syncsWithValidAdminKey() throws Exception {
    when(jobOperator.start(any(Job.class), any(JobParameters.class))).thenReturn(completedExecution());

    mockMvc.perform(post("/api/v1/admin/concerts/sync").header("X-Admin-Key", "test-admin-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.jobName").value("kopisConcertSyncJob"))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.readCount").value(10))
        .andExpect(jsonPath("$.data.writeCount").value(9))
        .andExpect(jsonPath("$.data.skipCount").value(1));
  }

  @Test
  @DisplayName("이미 Job이 실행 중이면 409를 반환한다")
  void returnsConflictWhenJobAlreadyRunning() throws Exception {
    when(jobOperator.start(any(Job.class), any(JobParameters.class))).thenThrow(new JobExecutionAlreadyRunningException("running"));

    mockMvc.perform(post("/api/v1/admin/concerts/sync").header("X-Admin-Key", "test-admin-key"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONCERT_SYNC_ALREADY_RUNNING"));
  }

  @Test
  @DisplayName("관리자 키 헤더가 없으면 거부한다")
  void rejectsMissingAdminKey() throws Exception {
    mockMvc.perform(post("/api/v1/admin/concerts/sync"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONCERT_ADMIN_ACCESS_DENIED"));

    verifyNoInteractions(jobOperator);
  }

  @Test
  @DisplayName("관리자 키가 틀리면 거부한다")
  void rejectsWrongAdminKey() throws Exception {
    mockMvc.perform(post("/api/v1/admin/concerts/sync").header("X-Admin-Key", "wrong-key"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONCERT_ADMIN_ACCESS_DENIED"));

    verifyNoInteractions(jobOperator);
  }

  private JobExecution completedExecution() {
    JobInstance jobInstance = new JobInstance(1L, "kopisConcertSyncJob");
    JobExecution execution = new JobExecution(1L, jobInstance, new JobParameters());
    execution.setStatus(BatchStatus.COMPLETED);
    execution.setExitStatus(ExitStatus.COMPLETED);
    execution.setStartTime(LocalDateTime.now());
    execution.setEndTime(LocalDateTime.now());

    StepExecution stepExecution = new StepExecution(1L, "concertSyncStep", execution);
    stepExecution.setReadCount(10);
    stepExecution.setWriteCount(9);
    stepExecution.setWriteSkipCount(1);
    execution.addStepExecution(stepExecution);

    return execution;
  }
}
