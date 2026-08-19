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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.global.batch.JobRunResult;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;
import com.fanroute.sync.global.config.AdminProperties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 역할 기반 인가를 도입하기 전까지 고정 관리자 키로 보호합니다. */
@RestController
@RequestMapping("/api/v1/admin/places")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "장소 관리", description = "관리자 전용 TourAPI 동기화 API")
public class PlaceAdminController {

  private static final String ADMIN_KEY_HEADER = "X-Admin-Key";

  private final JobOperator jobOperator;
  private final Job accommodationPlaceSyncJob;
  private final Job attractionPlaceSyncJob;
  private final Job restaurantPlaceSyncJob;
  private final AdminProperties adminProperties;

  @Operation(
      summary = "TourAPI 장소 수동 동기화",
      description = "관리자 키로 인증된 요청만 TourAPI 장소 동기화 Job을 즉시 실행합니다. "
          + "category를 지정하면 해당 Job만, 생략하면 카테고리별 Job을 격리된 상태로 모두 실행합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200", description = "동기화 실행 성공", useReturnTypeSchema = true)
  })
  @ApiErrorCodeExamples(
      type = PlaceErrorCode.class,
      names = {"ADMIN_ACCESS_DENIED", "SYNC_ALREADY_RUNNING"})
  @Parameter(
      name = ADMIN_KEY_HEADER, description = "관리자 전용 API 키", in = ParameterIn.HEADER,
      required = true)
  @PostMapping("/sync")
  public ResponseEntity<ApiResponse<List<JobRunResult>>> syncPlaces(
      @Parameter(hidden = true)
      @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String adminKey,
      @Parameter(description = "동기화할 카테고리. 생략 시 전체 카테고리를 격리된 상태로 동기화")
      @RequestParam(required = false) PlaceCategory category) {
    validateAdminKey(adminKey);
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

  private void validateAdminKey(String adminKey) {
    if (!adminProperties.apiKey().equals(adminKey)) {
      throw new BusinessException(PlaceErrorCode.ADMIN_ACCESS_DENIED);
    }
  }
}
