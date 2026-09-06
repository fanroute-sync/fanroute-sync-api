package com.fanroute.sync.domain.schedule.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.support.AbstractRepositoryTest;
import com.fanroute.sync.support.UserFixture;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

class AiItineraryGenerationRepositoryTest extends AbstractRepositoryTest {

  @Autowired
  private AiItineraryGenerationRepository generationRepository;

  @PersistenceContext
  private EntityManager entityManager;

  @Test
  @DisplayName("PENDING 또는 lease가 만료된 PROCESSING 작업만 선점한다")
  void acquiresPendingOrExpiredProcessingGeneration() {
    AiItineraryGeneration generation = persistGeneration();
    Instant now = Instant.parse("2026-09-01T00:00:00Z");

    assertThat(acquire(generation.getId(), now, now.plusSeconds(45))).isEqualTo(1);
    assertThat(acquire(generation.getId(), now.plusSeconds(30), now.plusSeconds(75)))
        .isZero();
    assertThat(acquire(generation.getId(), now.plusSeconds(46), now.plusSeconds(91)))
        .isEqualTo(1);

    entityManager.clear();
    AiItineraryGeneration reacquired = generationRepository.findById(generation.getId())
        .orElseThrow();
    assertThat(reacquired.getStatus()).isEqualTo(AiItineraryGenerationStatus.PROCESSING);
    assertThat(reacquired.getProcessingLeaseUntil()).isEqualTo(now.plusSeconds(91));
    assertThat(reacquired.getAttemptCount()).isEqualTo(2);
  }

  private int acquire(Long generationId, Instant now, Instant leaseUntil) {
    int updated = generationRepository.acquireForProcessing(generationId,
        AiItineraryGenerationStatus.PENDING, AiItineraryGenerationStatus.PROCESSING,
        now, leaseUntil);
    entityManager.flush();
    entityManager.clear();
    return updated;
  }

  private AiItineraryGeneration persistGeneration() {
    User user = UserFixture.activeUser();
    entityManager.persist(user);
    TripPlan tripPlan = TripPlan.create(user, null,
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T00:00:00Z"),
        null, List.of(), List.of());
    entityManager.persist(tripPlan);
    ItineraryDay day = ItineraryDay.create(tripPlan, LocalDate.of(2026, 9, 1), false);
    entityManager.persist(day);
    AiItineraryGeneration generation = AiItineraryGeneration.create(day);
    entityManager.persist(generation);
    entityManager.flush();
    return generation;
  }
}
