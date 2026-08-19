package com.fanroute.sync.domain.concert.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fanroute.sync.domain.concert.client.KopisClient;
import com.fanroute.sync.domain.concert.dto.KopisDto;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;
import com.fanroute.sync.support.TestContainerConfig;

@SpringBootTest(properties = {
    "auth.google.client-id=test-google-client",
    "auth.google.client-secret=test-google-secret",
    "auth.google.redirect-uri=http://localhost/test/callback",
    "auth.jwt.secret=dGVzdC1vbmx5LWtleS10aGF0LWlzLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=",
    "kopis.service-key=test-kopis-key",
    "tour-api.service-key=test-tour-key"
})
@Import(TestContainerConfig.class)
@SpringBatchTest
class ConcertSyncJobIntegrationTest {

  @Autowired
  private JobOperatorTestUtils jobOperatorTestUtils;
  @Autowired
  @Qualifier("kopisConcertSyncJob")
  private Job kopisConcertSyncJob;
  @Autowired
  private ConcertRepository concertRepository;
  @Autowired
  private VenueRepository venueRepository;
  @MockitoBean
  private KopisClient kopisClient;

  @BeforeEach
  void setUp() {
    jobOperatorTestUtils.setJob(kopisConcertSyncJob);
  }

  @Test
  @DisplayName("Job을 실행하면 Reader→Processor→Writer가 이어져 Venue·Concert가 실제로 저장된다")
  void runsJobEndToEnd() throws Exception {
    when(kopisClient.getPerformances(
        anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(new KopisDto.PerformanceListResponse(List.of(summary())));
    when(kopisClient.getPerformanceDetail(eq("PF001"), anyString()))
        .thenReturn(new KopisDto.PerformanceDetailResponse(detail()));
    when(kopisClient.getVenueDetail(eq("FC001"), anyString()))
        .thenReturn(new KopisDto.VenueDetailResponse(venue()));

    JobExecution execution = jobOperatorTestUtils.startJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(concertRepository.findByKopisConcertId("PF001"))
        .hasValueSatisfying(concert -> assertThat(concert.getTitle()).isEqualTo("테스트 공연"));
    assertThat(venueRepository.findByKopisVenueId("FC001"))
        .hasValueSatisfying(v -> assertThat(v.getName()).isEqualTo("테스트홀"));
  }

  private KopisDto.PerformanceSummary summary() {
    return new KopisDto.PerformanceSummary(
        "PF001", "테스트 공연", "2026.09.01", "2026.09.02", "테스트홀", "poster.jpg", "대중음악");
  }

  private KopisDto.PerformanceDetail detail() {
    return new KopisDto.PerformanceDetail(
        "PF001", "FC001", "테스트 공연", "2026.09.01", "2026.09.02", "테스트홀", "poster.jpg",
        "대중음악", "공연중");
  }

  private KopisDto.VenueDetail venue() {
    return new KopisDto.VenueDetail("FC001", "테스트홀", "부산 해운대구", 35.1, 129.0);
  }
}
