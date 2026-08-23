package com.fanroute.sync.domain.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.schedule.entity.ItineraryDay;

public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {
}
