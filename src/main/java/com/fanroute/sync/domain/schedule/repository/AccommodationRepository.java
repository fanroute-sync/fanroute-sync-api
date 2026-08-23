package com.fanroute.sync.domain.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.schedule.entity.Accommodation;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

  void deleteByTripPlanId(Long tripPlanId);
}
