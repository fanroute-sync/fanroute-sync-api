package com.fanroute.sync.domain.concert.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.concert.entity.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {

  Optional<Venue> findByKopisVenueId(String kopisVenueId);
}
