package com.fanroute.sync.global.batch.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fanroute.sync.global.batch.BatchJobExecutionRepository;
import com.fanroute.sync.global.batch.JobRunResult;
import com.fanroute.sync.global.config.SecurityConfig;
import com.fanroute.sync.support.SecurityWebMvcTestSupport;

@WebMvcTest(BatchJobHistoryController.class)
@Import({ SecurityConfig.class, SecurityWebMvcTestSupport.class })
class BatchJobHistoryControllerTest {

  private static final String JOB_NAME = "kopisConcertSyncJob";

  @Autowired
  private MockMvc mockMvc;
  @MockitoBean
  private BatchJobExecutionRepository batchJobExecutionRepository;
  @MockitoBean
  private Job kopisConcertSyncJob;

  private static RequestPostProcessor adminJwt() {
    return jwt().jwt(jwt -> jwt.subject("1")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  @Test
  @DisplayName("등록된 Job의 실행 이력을 조회한다")
  void returnsExecutionHistory() throws Exception {
    when(kopisConcertSyncJob.getName()).thenReturn(JOB_NAME);
    JobRunResult result = new JobRunResult(
        JOB_NAME, "COMPLETED", "COMPLETED", 10, 9, 1,
        LocalDateTime.now(), LocalDateTime.now());
    when(batchJobExecutionRepository.findByJobName(eq(JOB_NAME), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 20), 1));

    mockMvc.perform(get("/api/v1/admin/jobs/{jobName}/executions", JOB_NAME).with(adminJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].jobName").value(JOB_NAME))
        .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.content[0].readCount").value(10))
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  @DisplayName("등록되지 않은 Job 이름을 조회하면 404를 반환하고 저장소를 조회하지 않는다")
  void rejectsUnknownJobName() throws Exception {
    when(kopisConcertSyncJob.getName()).thenReturn(JOB_NAME);

    mockMvc.perform(get("/api/v1/admin/jobs/{jobName}/executions", "unknownJob").with(adminJwt()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BATCH_JOB_NOT_FOUND"));

    verifyNoInteractions(batchJobExecutionRepository);
  }

  @Test
  @DisplayName("ADMIN 권한이 없는 사용자는 접근을 거부당한다")
  void rejectsNonAdminUser() throws Exception {
    mockMvc.perform(get("/api/v1/admin/jobs/{jobName}/executions", JOB_NAME)
        .with(jwt().jwt(jwt -> jwt.subject("1"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
  }

  @Test
  @DisplayName("인증되지 않은 요청은 거부한다")
  void rejectsUnauthenticatedRequest() throws Exception {
    mockMvc.perform(get("/api/v1/admin/jobs/{jobName}/executions", JOB_NAME))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
  }
}
