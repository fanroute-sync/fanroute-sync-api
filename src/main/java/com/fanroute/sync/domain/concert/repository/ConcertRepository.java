package com.fanroute.sync.domain.concert.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.concert.entity.Concert;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

  Optional<Concert> findByKopisConcertId(String kopisConcertId);

  Page<Concert> findByEndDateGreaterThanEqualAndGenreName(
      LocalDate from, String genreName, Pageable pageable);

  Page<Concert> findByEndDateGreaterThanEqual(LocalDate from, Pageable pageable);
}
