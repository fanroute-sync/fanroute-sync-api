package com.fanroute.sync.domain.place.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fanroute.sync.domain.place.dto.TourApiDto.PlaceSummary;
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

import com.fanroute.sync.domain.place.client.TourApiClient;
import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.repository.PlaceRepository;
import com.fanroute.sync.support.TestContainerConfig;

@SpringBootTest(properties = {
    "auth.google.client-id=test-google-client",
    "auth.google.client-secret=test-google-secret",
    "auth.google.redirect-uri=http://localhost/test/callback",
    "auth.jwt.secret=dGVzdC1vbmx5LWtleS10aGF0LWlzLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=",
    "kopis.service-key=test-kopis-key",
    "tour-api.service-key=test-tour-key",
    "cors.allowed-origins=http://localhost",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.data.redis.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(TestContainerConfig.class)
@SpringBatchTest
class PlaceSyncJobIntegrationTest {

  @Autowired
  private JobOperatorTestUtils jobOperatorTestUtils;
  @Autowired
  @Qualifier("accommodationPlaceSyncJob")
  private Job accommodationPlaceSyncJob;
  @Autowired
  private PlaceRepository placeRepository;
  @MockitoBean
  private TourApiClient tourApiClient;

  @BeforeEach
  void setUp() {
    jobOperatorTestUtils.setJob(accommodationPlaceSyncJob);
  }

  @Test
  @DisplayName("Job을 실행하면 Reader→Processor→Writer가 이어져 장소가 실제로 저장된다")
  void runsJobEndToEnd() throws Exception {
    when(tourApiClient.searchStay(
        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(),
        eq(1)))
        .thenReturn(searchStayResponse(List.of(summary())));

    JobExecution execution = jobOperatorTestUtils.startJob();

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(placeRepository.findByContentId("126508"))
        .hasValueSatisfying(place -> {
          assertThat(place.getCategory()).isEqualTo(PlaceCategory.ACCOMMODATION);
          assertThat(place.getName()).isEqualTo("테스트 호텔");
        });
  }

  private TourApiDto.SearchStayResponse searchStayResponse(
      List<PlaceSummary> items) {
    TourApiDto.Items wrappedItems = new TourApiDto.Items(items);
    TourApiDto.Body body = new TourApiDto.Body(wrappedItems, items.size(), 1, 1);
    TourApiDto.Header header = new TourApiDto.Header(TourApiDto.SUCCESS_RESULT_CODE, "OK");
    return new TourApiDto.SearchStayResponse(new TourApiDto.Response(header, body));
  }

  private TourApiDto.PlaceSummary summary() {
    return new TourApiDto.PlaceSummary(
        "126508", "32", "테스트 호텔", "부산 해운대구", null, null, 129.163, 35.163, null, null, null,
        null, "26", null, null, null, null, null, null, null);
  }
}
