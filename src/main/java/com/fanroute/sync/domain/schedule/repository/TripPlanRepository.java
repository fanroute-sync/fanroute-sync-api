package com.fanroute.sync.domain.schedule.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.schedule.entity.TripPlan;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {

  /** 목록·상세 DTO 변환에서 사용하는 Concert를 함께 조회해 N+1을 방지합니다. */
  @EntityGraph(attributePaths = "concert")
  List<TripPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

  @EntityGraph(attributePaths = "concert")
  Optional<TripPlan> findByIdAndUserId(Long id, Long userId);
}
