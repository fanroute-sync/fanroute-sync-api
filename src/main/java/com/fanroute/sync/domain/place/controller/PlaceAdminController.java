package com.fanroute.sync.domain.place.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.place.dto.PlaceDto;
import com.fanroute.sync.domain.place.dto.TourApiDto;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.domain.place.service.PlaceAdminService;
import com.fanroute.sync.domain.place.service.PlaceService;
import com.fanroute.sync.global.batch.JobRunResult;
import com.fanroute.sync.global.batch.BatchJobLauncher;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PlaceAdminController implements PlaceAdminApi {

  private final BatchJobLauncher batchJobLauncher;
  private final PlaceAdminService placeAdminService;
  private final PlaceService placeService;
  private final Job accommodationPlaceSyncJob;
  private final Job attractionPlaceSyncJob;
  private final Job restaurantPlaceSyncJob;

  @Override
  public ResponseEntity<ApiResponse<List<JobRunResult>>> syncPlaces(PlaceCategory category) {
    List<JobRunResult> results = category != null
        ? List.of(launchOrThrow(category))
        : launchAllIsolated();
    return ApiResponse.ok(results).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<List<PlaceDto.NearbyCandidateResponse>>> getNearbyCandidates(
      PlaceCategory category, double latitude, double longitude, int radiusMeters) {
    List<TourApiDto.PlaceSummary> candidates =
        placeAdminService.searchNearbyCandidates(category, latitude, longitude, radiusMeters);
    List<PlaceDto.NearbyCandidateResponse> responses = candidates.stream()
        .map(summary -> PlaceDto.NearbyCandidateResponse.from(summary, existingPlaceId(summary)))
        .toList();
    return ApiResponse.ok(responses).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<PlaceDto.Response>> updateTags(
      Long placeId, PlaceDto.UpdateTagsRequest request) {
    return ApiResponse.ok(PlaceDto.Response.from(placeService.updateTags(placeId, request.tags())))
        .toResponseEntity();
  }

  private Long existingPlaceId(TourApiDto.PlaceSummary summary) {
    Place place = placeAdminService.findExistingByContentId(summary.contentId());
    return place == null ? null : place.getId();
  }

  private JobRunResult launchOrThrow(PlaceCategory category) {
    try {
      return JobRunResult.from(batchJobLauncher.start(jobFor(category), triggerParameters()));
    } catch (JobExecutionAlreadyRunningException | JobRestartException
             | JobInstanceAlreadyCompleteException | InvalidJobParametersException exception) {
      throw new BusinessException(PlaceErrorCode.SYNC_ALREADY_RUNNING);
    }
  }

  private List<JobRunResult> launchAllIsolated() {
    List<JobRunResult> results = new ArrayList<>();
    for (PlaceCategory category : PlaceCategory.values()) {
      Job job = jobFor(category);
      try {
        results.add(JobRunResult.from(batchJobLauncher.start(job, triggerParameters())));
      } catch (JobExecutionAlreadyRunningException | JobRestartException
               | JobInstanceAlreadyCompleteException | InvalidJobParametersException exception) {
        log.warn("TourAPI 장소 수동 동기화 실행 실패: category={}", category, exception);
        results.add(JobRunResult.failed(job.getName(), PlaceErrorCode.SYNC_ALREADY_RUNNING.getCode()));
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
