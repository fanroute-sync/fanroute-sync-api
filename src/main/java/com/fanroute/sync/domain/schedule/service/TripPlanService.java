package com.fanroute.sync.domain.schedule.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.service.ConcertService;
import com.fanroute.sync.domain.schedule.dto.TripPlanDto;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.ItineraryDayRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryItemRepository;
import com.fanroute.sync.domain.schedule.repository.AccommodationRepository;
import com.fanroute.sync.domain.schedule.repository.TripPlanRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TripPlanService {

  private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

  private final TripPlanRepository tripPlanRepository;
  private final ItineraryDayRepository itineraryDayRepository;
  private final ItineraryItemRepository itineraryItemRepository;
  private final AccommodationRepository accommodationRepository;
  private final ConcertService concertService;

  public TripPlanDto.CreateResponse create(User user, TripPlanDto.CreateRequest request) {
    Instant arrivalAt = toInstant(request.arrivalDate(), request.arrivalTimeSlot());
    Instant departureAt = toInstant(request.departureDate(), request.departureTimeSlot());
    if (arrivalAt.isAfter(departureAt)) {
      throw new BusinessException(ScheduleErrorCode.INVALID_TRIP_PERIOD);
    }

    Concert concert = request.concertId() == null ? null : concertService.getConcert(request.concertId());
    TripPlan tripPlan = tripPlanRepository.save(TripPlan.create(
        user, concert, arrivalAt, departureAt, null, List.of(), List.of()));
    List<ItineraryDay> itineraryDays = createItineraryDays(tripPlan, request, concert);

    return new TripPlanDto.CreateResponse(tripPlan.getId(), request.concertId(),
        arrivalAt, departureAt, itineraryDays.stream()
            .map(day -> new TripPlanDto.ItineraryDayResponse(day.getId(), day.getDate(),
                day.isConcertDay()))
            .toList());
  }

  @Transactional(readOnly = true)
  public List<TripPlanDto.SummaryResponse> getMyTripPlans(User user) {
    return tripPlanRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
        .map(this::toSummary).toList();
  }

  @Transactional(readOnly = true)
  public TripPlanDto.DetailResponse getTripPlan(User user, Long tripPlanId) {
    TripPlan tripPlan = getOwnedTripPlan(user, tripPlanId);
    List<TripPlanDto.ItineraryDayResponse> days = itineraryDayRepository
        .findByTripPlanIdOrderByDateAsc(tripPlanId).stream()
        .map(day -> new TripPlanDto.ItineraryDayResponse(day.getId(), day.getDate(), day.isConcertDay()))
        .toList();
    return new TripPlanDto.DetailResponse(tripPlan.getId(), concertId(tripPlan), concertTitle(tripPlan),
        tripPlan.getArrivalAt(), tripPlan.getDepartureAt(), days);
  }

  public void deleteTripPlan(User user, Long tripPlanId) {
    getOwnedTripPlan(user, tripPlanId);
    itineraryItemRepository.deleteByItineraryDayTripPlanId(tripPlanId);
    itineraryDayRepository.deleteByTripPlanId(tripPlanId);
    accommodationRepository.deleteByTripPlanId(tripPlanId);
    tripPlanRepository.deleteById(tripPlanId);
  }

  private TripPlan getOwnedTripPlan(User user, Long tripPlanId) {
    return tripPlanRepository.findByIdAndUserId(tripPlanId, user.getId())
        .orElseThrow(() -> new BusinessException(ScheduleErrorCode.TRIP_PLAN_NOT_FOUND));
  }

  private TripPlanDto.SummaryResponse toSummary(TripPlan tripPlan) {
    return new TripPlanDto.SummaryResponse(tripPlan.getId(), concertId(tripPlan), concertTitle(tripPlan),
        tripPlan.getArrivalAt(), tripPlan.getDepartureAt());
  }

  private Long concertId(TripPlan tripPlan) {
    return tripPlan.getConcert() == null ? null : tripPlan.getConcert().getId();
  }

  private String concertTitle(TripPlan tripPlan) {
    return tripPlan.getConcert() == null ? null : tripPlan.getConcert().getTitle();
  }

  private List<ItineraryDay> createItineraryDays(TripPlan tripPlan, TripPlanDto.CreateRequest request,
      Concert concert) {
    List<ItineraryDay> days = new ArrayList<>();
    for (LocalDate date = request.arrivalDate(); !date.isAfter(request.departureDate());
        date = date.plusDays(1)) {
      boolean concertDay = concert != null && !date.isBefore(concert.getStartDate())
          && !date.isAfter(concert.getEndDate());
      days.add(ItineraryDay.create(tripPlan, date, concertDay));
    }
    return itineraryDayRepository.saveAll(days);
  }

  private Instant toInstant(LocalDate date, com.fanroute.sync.domain.schedule.entity.TravelTimeSlot slot) {
    return date.atTime(slot.getTime()).atZone(KOREA_ZONE_ID).toInstant();
  }
}
