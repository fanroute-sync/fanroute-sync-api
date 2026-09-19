package com.fanroute.sync.domain.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.fanroute.sync.domain.schedule.entity.Accommodation;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

  void deleteByTripPlanId(Long tripPlanId);

  List<Accommodation> findByTripPlanIdOrderByCheckinDateAsc(Long tripPlanId);

  Optional<Accommodation> findByIdAndTripPlanId(Long accommodationId, Long tripPlanId);

  boolean existsByTripPlanIdAndCheckinDateLessThanAndCheckoutDateGreaterThan(Long tripPlanId,
      LocalDate checkoutDate, LocalDate checkinDate);
}
