package com.fanroute.sync.domain.place.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.global.batch.JobRunResult;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin/places")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "장소 관리", description = "관리자 전용 TourAPI 동기화 API")
@SecurityRequirement(name = "bearerAuth")
public class PlaceAdminController {

  private final JobOperator jobOperator;
  private final Job accommodationPlaceSyncJob;
  private final Job attractionPlaceSyncJob;
  private final Job restaurantPlaceSyncJob;

  @Operation(
      summary = "TourAPI 장소 수동 동기화",
      description = "ADMIN 권한을 가진 사용자만 TourAPI 장소 동기화 Job을 즉시 실행합니다. "
          + "category를 지정하면 해당 Job만, 생략하면 카테고리별 Job을 격리된 상태로 모두 실행합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "동기화 실행 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = PlaceErrorCode.class,
      names = {"SYNC_ALREADY_RUNNING"})
  @ApiErrorCodeExamples(
      type = AuthErrorCode.class,
      names = {"ACCESS_DENIED"})
  @PostMapping("/sync")
  public ResponseEntity<ApiResponse<List<JobRunResult>>> syncPlaces(
      @Parameter(description = "동기화할 카테고리. 생략 시 전체 카테고리를 격리된 상태로 동기화")
      @RequestParam(required = false) PlaceCategory category) {
    List<JobRunResult> results = category != null
        ? List.of(launchOrThrow(category))
        : launchAllIsolated();
    return ApiResponse.ok(results).toResponseEntity();
  }

  private JobRunResult launchOrThrow(PlaceCategory category) {
    try {
      return JobRunResult.from(jobOperator.start(jobFor(category), triggerParameters()));
    } catch (JobExecutionAlreadyRunningException | JobRestartException
             | JobInstanceAlreadyCompleteException | InvalidJobParametersException exception) {
      throw new BusinessException(PlaceErrorCode.SYNC_ALREADY_RUNNING);
    }
  }

  private List<JobRunResult> launchAllIsolated() {
    List<JobRunResult> results = new ArrayList<>();
    for (PlaceCategory category : PlaceCategory.values()) {
      try {
        results.add(JobRunResult.from(jobOperator.start(jobFor(category), triggerParameters())));
      } catch (JobExecutionAlreadyRunningException | JobRestartException
               | JobInstanceAlreadyCompleteException | InvalidJobParametersException exception) {
        log.warn("TourAPI 장소 수동 동기화 실행 실패: category={}", category, exception);
      }
    }
    return results;
  }

  private JobParameters triggerParameters() {
    return new JobParametersBuilder()
        .addLong("triggeredAt", System.currentTimeMillis())
        .toJobParameters();
  }

  private Job jobFor(PlaceCategory category) {
    return switch (category) {
      case ACCOMMODATION -> accommodationPlaceSyncJob;
      case ATTRACTION -> attractionPlaceSyncJob;
      case RESTAURANT -> restaurantPlaceSyncJob;
    };
  }
}