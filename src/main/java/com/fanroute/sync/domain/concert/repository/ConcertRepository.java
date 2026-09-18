package com.fanroute.sync.domain.concert.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.Genre;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

  Optional<Concert> findByKopisConcertId(String kopisConcertId);

  long countByVenueIdAndEndDateGreaterThanEqual(Long venueId, LocalDate from);

  /** OSIV가 꺼진 환경에서 DTO 변환 시 접근할 venue를 함께 조회합니다. */
  @Query("select c from Concert c join fetch c.venue where c.id = :id")
  Optional<Concert> findWithVenueById(@Param("id") Long id);

  @Query(
      value = "select c from Concert c join fetch c.venue "
          + "where c.endDate >= :from and c.genreName = :genreName",
      countQuery = "select count(c) from Concert c "
          + "where c.endDate >= :from and c.genreName = :genreName")
  Page<Concert> findByEndDateGreaterThanEqualAndGenreName(
      @Param("from") LocalDate from, @Param("genreName") Genre genreName, Pageable pageable);

  @Query(
      value = "select c from Concert c join fetch c.venue where c.endDate >= :from",
      countQuery = "select count(c) from Concert c where c.endDate >= :from")
  Page<Concert> findByEndDateGreaterThanEqual(@Param("from") LocalDate from, Pageable pageable);
}
