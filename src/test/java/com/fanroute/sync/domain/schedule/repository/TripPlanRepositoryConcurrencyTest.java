package com.fanroute.sync.domain.schedule.repository;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.repository.UserRepository;
import com.fanroute.sync.support.AbstractRepositoryTest;

class TripPlanRepositoryConcurrencyTest extends AbstractRepositoryTest {

  @Autowired
  private TripPlanRepository tripPlanRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DisplayName("같은 여행 일정의 동시 AI 예약은 5개까지만 성공한다")
  void reserveAiGenerationConcurrently() throws Exception {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    Long tripPlanId = transactionTemplate.execute(status -> createTripPlan());
    int requestCount = TripPlan.AI_GENERATION_LIMIT + 1;
    CountDownLatch ready = new CountDownLatch(requestCount);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(requestCount);

    try {
      List<Future<Integer>> futures = new ArrayList<>();
      for (int index = 0; index < requestCount; index++) {
        futures.add(executor.submit(() -> {
          ready.countDown();
          if (!start.await(5, SECONDS)) {
            throw new IllegalStateException("Concurrent reservation start timed out");
          }
          return transactionTemplate.execute(status -> tripPlanRepository.reserveAiGeneration(
              tripPlanId, TripPlan.AI_GENERATION_LIMIT));
        }));
      }

      assertThat(ready.await(5, SECONDS)).isTrue();
      start.countDown();

      List<Integer> results = new ArrayList<>();
      for (Future<Integer> future : futures) {
        results.add(future.get(10, SECONDS));
      }

      assertThat(results).containsExactlyInAnyOrder(1, 1, 1, 1, 1, 0);
      UsageCount usageCount = transactionTemplate.execute(status -> tripPlanRepository
          .findById(tripPlanId)
          .map(plan -> new UsageCount(
              plan.getAiGenerationUsedCount(), plan.getAiGenerationReservedCount()))
          .orElseThrow());
      assertThat(usageCount).isEqualTo(new UsageCount(0, TripPlan.AI_GENERATION_LIMIT));
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, SECONDS)).isTrue();
    }
  }

  private Long createTripPlan() {
    User user = userRepository.save(User.create(
        "concurrency-user", AuthProvider.GOOGLE, "concurrency-provider-user"));
    TripPlan tripPlan = TripPlan.create(
        user,
        null,
        Instant.parse("2026-09-01T00:00:00Z"),
        Instant.parse("2026-09-02T00:00:00Z"),
        null,
        List.of(),
        List.of());
    return tripPlanRepository.saveAndFlush(tripPlan).getId();
  }

  private record UsageCount(int used, int reserved) {
  }
}
