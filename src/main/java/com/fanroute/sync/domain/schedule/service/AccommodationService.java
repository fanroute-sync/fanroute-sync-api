package com.fanroute.sync.domain.schedule.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceCategory;
import com.fanroute.sync.domain.place.exception.PlaceErrorCode;
import com.fanroute.sync.domain.place.repository.PlaceRepository;
import com.fanroute.sync.domain.schedule.client.KakaoLocalClient;
import com.fanroute.sync.domain.schedule.dto.TripPlanDto;
import com.fanroute.sync.domain.schedule.entity.Accommodation;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.AccommodationRepository;
import com.fanroute.sync.domain.schedule.repository.TripPlanRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccommodationService {

  private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final TripPlanRepository tripPlanRepository;
  private final AccommodationRepository accommodationRepository;
  private final PlaceRepository placeRepository;
  private final KakaoLocalClient kakaoLocalClient;

  public List<TripPlanDto.AccommodationResponse> replace(User user, Long tripPlanId,
      TripPlanDto.ReplaceAccommodationsRequest request) {
    TripPlan tripPlan = getOwnedTripPlan(user, tripPlanId);
    validateStayPeriods(tripPlan, request.accommodations());
    accommodationRepository.deleteByTripPlanId(tripPlanId);
    List<Accommodation> accommodations = request.accommodations().stream()
        .map(item -> createAccommodation(tripPlan, item))
        .toList();
    return accommodationRepository.saveAll(accommodations).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<TripPlanDto.AccommodationResponse> getAll(User user, Long tripPlanId) {
    getOwnedTripPlan(user, tripPlanId);
    return accommodationRepository.findByTripPlanIdOrderByCheckinDateAsc(tripPlanId).stream()
        .map(this::toResponse)
        .toList();
  }

  private TripPlan getOwnedTripPlan(User user, Long tripPlanId) {
    return tripPlanRepository.findByIdAndUserId(tripPlanId, user.getId())
        .orElseThrow(() -> new BusinessException(ScheduleErrorCode.TRIP_PLAN_NOT_FOUND));
  }

  private void validateStayPeriods(TripPlan tripPlan,
      List<TripPlanDto.CreateAccommodationRequest> requests) {
    Set<LocalDate> occupiedDates = new HashSet<>();
    for (TripPlanDto.CreateAccommodationRequest request : requests) {
      validateStayPeriod(tripPlan, request.checkinDate(), request.checkoutDate());
      for (LocalDate date = request.checkinDate(); date.isBefore(request.checkoutDate());
          date = date.plusDays(1)) {
        if (!occupiedDates.add(date)) {
          throw new BusinessException(ScheduleErrorCode.INVALID_ACCOMMODATION);
        }
      }
    }
  }

  private void validateStayPeriod(TripPlan tripPlan, LocalDate checkinDate, LocalDate checkoutDate) {
    LocalDate tripStart = tripPlan.getArrivalAt().atZone(KOREA_ZONE_ID).toLocalDate();
    LocalDate tripEnd = tripPlan.getDepartureAt().atZone(KOREA_ZONE_ID).toLocalDate();
    if (checkinDate == null || checkoutDate == null || !checkinDate.isBefore(checkoutDate)
        || checkinDate.isBefore(tripStart) || checkoutDate.isAfter(tripEnd)) {
      throw new BusinessException(ScheduleErrorCode.INVALID_ACCOMMODATION);
    }
  }

  private Accommodation createAccommodation(TripPlan tripPlan,
      TripPlanDto.CreateAccommodationRequest request) {
    if (request.placeId() != null) {
      Place place = placeRepository.findById(request.placeId())
          .orElseThrow(() -> new BusinessException(PlaceErrorCode.PLACE_NOT_FOUND));
      if (place.getCategory() != PlaceCategory.ACCOMMODATION) {
        throw new BusinessException(ScheduleErrorCode.INVALID_ACCOMMODATION);
      }
      return Accommodation.create(tripPlan, place.getName(), place, place.getLatitude(),
          place.getLongitude(), request.checkinDate(), request.checkoutDate());
    }
    KakaoLocalClient.Coordinates coordinates = kakaoLocalClient
        .findCoordinates(request.nameOrAddress()).orElse(null);
    return Accommodation.create(tripPlan, request.nameOrAddress(), null,
        coordinates == null ? null : coordinates.latitude(),
        coordinates == null ? null : coordinates.longitude(), request.checkinDate(),
        request.checkoutDate());
  }

  private TripPlanDto.AccommodationResponse toResponse(Accommodation accommodation) {
    Long placeId = accommodation.getPlace() == null ? null : accommodation.getPlace().getId();
    return new TripPlanDto.AccommodationResponse(accommodation.getId(), placeId,
        accommodation.getNameOrAddress(), accommodation.getLatitude(), accommodation.getLongitude(),
        accommodation.getCheckinDate(), accommodation.getCheckoutDate());
  }
}
