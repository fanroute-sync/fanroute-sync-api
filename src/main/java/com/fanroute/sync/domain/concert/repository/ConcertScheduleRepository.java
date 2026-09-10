package com.fanroute.sync.domain.concert.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fanroute.sync.domain.concert.entity.ConcertSchedule;

public interface ConcertScheduleRepository extends JpaRepository<ConcertSchedule, Long> {

  @Query(
      "select s from ConcertSchedule s where s.concert.id = :concertId and s.deletedAt is null "
          + "order by s.performanceDate asc, s.performanceTime asc")
  List<ConcertSchedule> findByConcertId(@Param("concertId") Long concertId);

  @Query(
      "select s from ConcertSchedule s where s.concert.id = :concertId "
          + "and s.performanceDate = :performanceDate and s.deletedAt is null "
          + "order by s.performanceTime asc")
  List<ConcertSchedule> findByConcertIdAndPerformanceDate(
      @Param("concertId") Long concertId, @Param("performanceDate") LocalDate performanceDate);

  // 공연 기간 검증에 concert.getStartDate()/getEndDate()가 필요해 join fetch를 유지합니다.
  @Query(
      "select s from ConcertSchedule s join fetch s.concert "
          + "where s.id = :id and s.deletedAt is null")
  Optional<ConcertSchedule> findWithConcertById(@Param("id") Long id);
}
