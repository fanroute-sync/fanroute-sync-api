package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fanroute.sync.domain.schedule.client.GeminiDto;
import com.fanroute.sync.domain.schedule.client.GeminiRestClient;
import com.fanroute.sync.domain.schedule.config.AiGenerationRetryProperties;
import com.fanroute.sync.domain.schedule.config.AiGenerationStreamProperties;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.ItineraryItem;
import com.fanroute.sync.domain.schedule.entity.ItineraryItemType;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.repository.ItineraryItemRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.repository.UserRepository;
import com.fanroute.sync.support.AbstractRepositoryTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Import({AiItineraryGenerationService.class, AiItineraryGenerationAsyncService.class,
    AiPlaceCandidateRanker.class, AiGenerationStreamProperties.class,
    AiGenerationRetryPolicy.class, AiGenerationRetryProperties.class})
class AiItineraryTimeValidationIntegrationTest extends AbstractRepositoryTest {

  @Autowired
  private AiItineraryGenerationAsyncService asyncService;
  @Autowired
  private PlatformTransactionManager transactionManager;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ItineraryItemRepository itemRepository;
  @PersistenceContext
  private EntityManager entityManager;
  @MockitoBean
  private GeminiRestClient geminiRestClient;
  @MockitoBean
  private AiGenerationOutboxService outboxService;
  @MockitoBean
  private AiGenerationNotificationOutboxService notificationOutboxService;

  @ParameterizedTest
  @CsvSource({"13:30, FAILED, 1, 0", "14:00, COMPLETED, 2, 1"})
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DisplayName("모델 호출 중 저장된 수동 일정과 충돌하면 실패하며 경계 시각은 저장·차감한다")
  void validatesLatestCommittedItem(String time, AiItineraryGenerationStatus expectedStatus,
      int expectedItemCount, int expectedUsedCount) {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    Fixture fixture = transaction.execute(status -> persistFixture(time));
    when(geminiRestClient.generate(any())).thenAnswer(invocation -> {
      transaction.executeWithoutResult(status -> entityManager.persist(ItineraryItem.create(
          entityManager.find(ItineraryDay.class, fixture.dayId()), 1, LocalTime.of(13, 0),
          ItineraryItemType.CUSTOM, null, null, "새 수동 일정", 60)));
      return new GeminiDto.GenerationResult(new GeminiDto.GeneratedItinerary(List.of(
          new GeminiDto.GeneratedItem(time, "AI 산책", 60, null))), null);
    });

    assertThat(asyncService.generate(fixture.generationId()))
        .isEqualTo(AiItineraryGenerationAsyncService.ExecutionResult.ACKNOWLEDGE);

    transaction.executeWithoutResult(status -> {
      AiItineraryGeneration generation = entityManager.find(
          AiItineraryGeneration.class, fixture.generationId());
      User user = entityManager.find(User.class, fixture.userId());
      assertThat(generation.getStatus()).isEqualTo(expectedStatus);
      assertThat(user.getAiGenerationUsedCount()).isEqualTo(expectedUsedCount);
      assertThat(user.getAiGenerationReservedCount()).isZero();
      assertThat(itemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(
          fixture.dayId())).hasSize(expectedItemCount);
    });
  }

  private Fixture persistFixture(String suffix) {
    User user = User.create("time-validation-" + suffix, AuthProvider.GOOGLE,
        "time-validation-" + suffix);
    entityManager.persist(user);
    TripPlan trip = TripPlan.create(user, null, Instant.parse("2026-09-01T00:00:00Z"),
        Instant.parse("2026-09-01T09:00:00Z"), null, List.of(), List.of());
    entityManager.persist(trip);
    ItineraryDay day = ItineraryDay.create(trip, LocalDate.of(2026, 9, 1), false);
    entityManager.persist(day);
    AiItineraryGeneration generation = AiItineraryGeneration.create(day);
    entityManager.persist(generation);
    entityManager.flush();
    assertThat(userRepository.reserveAiGeneration(user.getId(), User.AI_GENERATION_LIMIT))
        .isEqualTo(1);
    return new Fixture(user.getId(), day.getId(), generation.getId());
  }

  private record Fixture(Long userId, Long dayId, Long generationId) {
  }
}
