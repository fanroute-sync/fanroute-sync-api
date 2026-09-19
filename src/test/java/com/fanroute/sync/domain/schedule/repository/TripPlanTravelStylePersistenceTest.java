package com.fanroute.sync.domain.schedule.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fanroute.sync.domain.schedule.entity.CompanionType;
import com.fanroute.sync.domain.schedule.entity.TravelMbtiType;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.support.AbstractRepositoryTest;
import com.fanroute.sync.support.UserFixture;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

class TripPlanTravelStylePersistenceTest extends AbstractRepositoryTest {

  @Autowired
  private TripPlanRepository tripPlanRepository;

  @PersistenceContext
  private EntityManager entityManager;

  @Test
  @DisplayName("여행 스타일 enum은 기존 한국어 라벨로 저장하고 다시 읽는다")
  void persistsTravelStyleLabels() {
    var user = UserFixture.activeUser();
    entityManager.persist(user);
    TripPlan tripPlan = TripPlan.create(user, null,
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T00:00:00Z"),
        null, List.of(CompanionType.FRIEND, CompanionType.CHILD), List.of());
    tripPlan.updateTravelStyle(null, List.of(CompanionType.FRIEND, CompanionType.CHILD),
        TravelMbtiType.FOOD_EXPLORER);
    entityManager.persist(tripPlan);
    entityManager.flush();
    entityManager.clear();

    TripPlan restored = tripPlanRepository.findById(tripPlan.getId()).orElseThrow();

    assertThat(restored.getCompanions()).containsExactly(CompanionType.FRIEND, CompanionType.CHILD);
    assertThat(restored.getTravelMbti()).isEqualTo(TravelMbtiType.FOOD_EXPLORER);
  }
}
