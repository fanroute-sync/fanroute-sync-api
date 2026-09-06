package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.repository.PlaceRepository;
import com.fanroute.sync.domain.schedule.client.GeminiDto;
import com.fanroute.sync.domain.schedule.config.AiGenerationStreamProperties;
import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.entity.AiGenerationDeadLetter;
import com.fanroute.sync.domain.schedule.entity.AiGenerationNotificationType;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.ItineraryItem;
import com.fanroute.sync.domain.schedule.entity.ItineraryItemType;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.AiItineraryGenerationRepository;
import com.fanroute.sync.domain.schedule.repository.AiGenerationDeadLetterRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryDayRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryItemRepository;
import com.fanroute.sync.domain.schedule.repository.TripPlanRepository;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.support.UserFixture;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AiItineraryGenerationServiceTest {

  @Mock
  private AiItineraryGenerationRepository generationRepository;
  @Mock
  private ItineraryDayRepository itineraryDayRepository;
  @Mock
  private ItineraryItemRepository itineraryItemRepository;
  @Mock
  private PlaceRepository placeRepository;
  @Mock
  private TripPlanRepository tripPlanRepository;
  @Mock
  private AiGenerationOutboxService outboxService;
  @Mock
  private AiGenerationRetryPolicy retryPolicy;
  @Mock
  private AiGenerationDeadLetterRepository deadLetterRepository;
  @Mock
  private AiGenerationNotificationOutboxService notificationOutboxService;

  @Test
  @DisplayName("내 날짜별 일정에 PENDING AI 생성 작업을 만든다")
  void requestsGeneration() {
    ItineraryDay itineraryDay = itineraryDay();
    when(itineraryDayRepository.findById(1L)).thenReturn(Optional.of(itineraryDay));
    when(tripPlanRepository.reserveAiGeneration(1L, TripPlan.AI_GENERATION_LIMIT)).thenReturn(1);
    when(generationRepository.save(any(AiItineraryGeneration.class))).thenAnswer(invocation -> {
      AiItineraryGeneration generation = invocation.getArgument(0);
      ReflectionTestUtils.setField(generation, "id", 10L);
      return generation;
    });

    AiItineraryGenerationDto.CreateResponse response = service().request(
        UserFixture.activeUserWithId(1L), 1L);

    assertThat(response.generationId()).isEqualTo(10L);
    assertThat(response.status()).isEqualTo(AiItineraryGenerationStatus.PENDING);
  }

  @Test
  @DisplayName("AI 추천 5회가 모두 예약된 여행 일정에는 새 생성 작업을 요청할 수 없다")
  void rejectsGenerationWhenTripPlanAiLimitIsReached() {
    when(itineraryDayRepository.findById(1L)).thenReturn(Optional.of(itineraryDay()));
    when(tripPlanRepository.reserveAiGeneration(1L, TripPlan.AI_GENERATION_LIMIT)).thenReturn(0);

    assertThatThrownBy(() -> service().request(UserFixture.activeUserWithId(1L), 1L))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.AI_ITINERARY_GENERATION_LIMIT_EXCEEDED);
    verify(generationRepository, never()).save(any());
  }

  @Test
  @DisplayName("공연일에는 AI 일정 생성 작업을 요청할 수 없다")
  void rejectsGenerationOnConcertDay() {
    ItineraryDay concertDay = itineraryDay(true);
    when(itineraryDayRepository.findById(1L)).thenReturn(Optional.of(concertDay));

    assertThatThrownBy(() -> service().request(UserFixture.activeUserWithId(1L), 1L))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.AI_ITINERARY_GENERATION_UNAVAILABLE_ON_CONCERT_DAY);
    verify(generationRepository, never()).save(any());
  }

  @Test
  @DisplayName("다른 사용자의 AI 생성 작업 상태는 조회할 수 없다")
  void rejectsStatusLookupForOtherUser() {
    when(generationRepository.findByIdAndItineraryDayTripPlanUserId(10L, 2L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getStatus(UserFixture.activeUserWithId(2L), 10L))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.AI_ITINERARY_GENERATION_NOT_FOUND);
  }

  @Test
  @DisplayName("실패한 AI 생성 작업은 새 PENDING 작업으로 재시도한다")
  void retriesFailedGeneration() {
    AiItineraryGeneration failedGeneration = generation(AiItineraryGenerationStatus.FAILED);
    when(generationRepository.findByIdAndItineraryDayTripPlanUserId(10L, 1L))
        .thenReturn(Optional.of(failedGeneration));
    when(tripPlanRepository.reserveAiGeneration(1L, TripPlan.AI_GENERATION_LIMIT)).thenReturn(1);
    when(generationRepository.save(any(AiItineraryGeneration.class))).thenAnswer(invocation -> {
      AiItineraryGeneration generation = invocation.getArgument(0);
      ReflectionTestUtils.setField(generation, "id", 11L);
      return generation;
    });

    AiItineraryGenerationDto.CreateResponse response = service().retry(
        UserFixture.activeUserWithId(1L), 10L);

    assertThat(response.generationId()).isEqualTo(11L);
    assertThat(response.status()).isEqualTo(AiItineraryGenerationStatus.PENDING);
    assertThat(failedGeneration.getStatus()).isEqualTo(AiItineraryGenerationStatus.FAILED);
  }

  @Test
  @DisplayName("공연일의 실패 작업은 재시도할 수 없다")
  void rejectsRetryOnConcertDay() {
    AiItineraryGeneration failedGeneration = generation(AiItineraryGenerationStatus.FAILED, true);
    when(generationRepository.findByIdAndItineraryDayTripPlanUserId(10L, 1L))
        .thenReturn(Optional.of(failedGeneration));

    assertThatThrownBy(() -> service().retry(UserFixture.activeUserWithId(1L), 10L))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.AI_ITINERARY_GENERATION_UNAVAILABLE_ON_CONCERT_DAY);
    verify(generationRepository, never()).save(any());
  }

  @Test
  @DisplayName("실패 상태가 아닌 AI 생성 작업은 재시도할 수 없다")
  void rejectsRetryForNonFailedGeneration() {
    when(generationRepository.findByIdAndItineraryDayTripPlanUserId(10L, 1L))
        .thenReturn(Optional.of(generation(AiItineraryGenerationStatus.PENDING)));

    assertThatThrownBy(() -> service().retry(UserFixture.activeUserWithId(1L), 10L))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.INVALID_AI_ITINERARY_GENERATION_STATUS);
  }

  @Test
  @DisplayName("대기 중인 AI 생성 작업을 취소한다")
  void cancelsPendingGeneration() {
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PENDING);
    when(generationRepository.findByIdAndItineraryDayTripPlanUserId(10L, 1L))
        .thenReturn(Optional.of(generation));

    AiItineraryGenerationDto.StatusResponse response = service().cancel(
        UserFixture.activeUserWithId(1L), 10L);

    assertThat(response.status()).isEqualTo(AiItineraryGenerationStatus.CANCELLED);
    assertThat(generation.getStatus()).isEqualTo(AiItineraryGenerationStatus.CANCELLED);
    verify(tripPlanRepository).releaseAiGeneration(1L);
  }

  @Test
  @DisplayName("대기 상태가 아닌 AI 생성 작업은 취소할 수 없다")
  void rejectsCancelForNonPendingGeneration() {
    when(generationRepository.findByIdAndItineraryDayTripPlanUserId(10L, 1L))
        .thenReturn(Optional.of(generation(AiItineraryGenerationStatus.FAILED)));

    assertThatThrownBy(() -> service().cancel(UserFixture.activeUserWithId(1L), 10L))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.INVALID_AI_ITINERARY_GENERATION_STATUS);
  }

  @Test
  @DisplayName("대기 중인 작업만 처리 상태로 선점하고 AI 입력을 만든다")
  void startsPendingGenerationAndBuildsInput(CapturedOutput output) {
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    ReflectionTestUtils.setField(generation, "attemptCount", 1);
    ReflectionTestUtils.setField(generation, "createdAt", Instant.now().minusSeconds(1));
    ItineraryItem concertItem = ItineraryItem.create(itineraryDay(), 1, LocalTime.of(19, 0),
        ItineraryItemType.CONCERT, null, org.mockito.Mockito.mock(Concert.class), "공연", 120);
    when(generationRepository.acquireForProcessing(eq(10L),
        eq(AiItineraryGenerationStatus.PENDING),
        eq(AiItineraryGenerationStatus.PROCESSING), any(Instant.class), any(Instant.class)))
        .thenReturn(1);
    when(generationRepository.findById(10L)).thenReturn(Optional.of(generation));
    when(itineraryItemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(1L))
        .thenReturn(List.of(concertItem));
    when(placeRepository.findTop20ByOrderByIdAsc()).thenReturn(List.of());

    AiItineraryGenerationDto.GenerationInput input = service().start(10L);

    assertThat(input.date()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(input.fixedItems()).singleElement()
        .extracting(AiItineraryGenerationDto.FixedItem::scheduledTime)
        .isEqualTo(LocalTime.of(19, 0));
    assertThat(output).contains("generationId=10", "attempt=1", "queueWaitMs=");
  }

  @Test
  @DisplayName("기존 일정에 등록된 장소는 AI 장소 후보에서 제외한다")
  void excludesExistingPlacesFromCandidates() {
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    Place existingPlace = place(1L);
    Place candidatePlace = place(2L);
    when(candidatePlace.getName()).thenReturn("해운대 해수욕장");
    ItineraryItem existingItem = ItineraryItem.create(itineraryDay(), 1, LocalTime.of(10, 0),
        ItineraryItemType.PLACE, existingPlace, null, "광안리 해수욕장", 60);
    when(generationRepository.acquireForProcessing(eq(10L),
        eq(AiItineraryGenerationStatus.PENDING),
        eq(AiItineraryGenerationStatus.PROCESSING), any(Instant.class), any(Instant.class)))
        .thenReturn(1);
    when(generationRepository.findById(10L)).thenReturn(Optional.of(generation));
    when(itineraryItemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(1L))
        .thenReturn(List.of(existingItem));
    when(placeRepository.findTop20ByOrderByIdAsc()).thenReturn(List.of(existingPlace, candidatePlace));

    AiItineraryGenerationDto.GenerationInput input = service().start(10L);

    assertThat(input.placeCandidates()).extracting(AiItineraryGenerationDto.PlaceCandidate::id)
        .containsExactly(2L);
  }

  @Test
  @DisplayName("이미 처리된 작업은 다시 시작하지 않는다")
  void doesNotStartAlreadyClaimedGeneration() {
    when(generationRepository.acquireForProcessing(eq(10L),
        eq(AiItineraryGenerationStatus.PENDING),
        eq(AiItineraryGenerationStatus.PROCESSING), any(Instant.class), any(Instant.class)))
        .thenReturn(0);

    assertThat(service().start(10L)).isNull();
    verify(generationRepository, never()).findById(any());
  }

  @Test
  @DisplayName("종료 상태 작업은 중복 Stream 메시지를 ACK할 수 있다")
  void identifiesTerminalGeneration() {
    when(generationRepository.findById(10L))
        .thenReturn(Optional.of(generation(AiItineraryGenerationStatus.COMPLETED)));

    assertThat(service().isTerminal(10L)).isTrue();
  }

  @Test
  @DisplayName("검증된 Gemini 결과를 장소 참조와 함께 일반 일정으로 저장한다")
  void savesGeneratedItineraryItems() {
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    Place place = org.mockito.Mockito.mock(Place.class);
    when(place.getId()).thenReturn(2L);
    when(place.getName()).thenReturn("해운대 해수욕장");
    when(generationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(generation));
    when(itineraryItemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(1L))
        .thenReturn(List.of());
    when(placeRepository.findAllById(List.of(2L))).thenReturn(List.of(place));
    when(tripPlanRepository.confirmAiGeneration(1L)).thenReturn(1);

    service().complete(10L, inputWithPlaceCandidate(), new GeminiDto.GeneratedItinerary(List.of(
        new GeminiDto.GeneratedItem("14:00", "임의 제목", 90, 2L),
        new GeminiDto.GeneratedItem("11:00", "카페", 60, null))));

    ArgumentCaptor<Iterable<ItineraryItem>> itemsCaptor = ArgumentCaptor.captor();
    verify(itineraryItemRepository).saveAll(itemsCaptor.capture());
    List<ItineraryItem> savedItems = StreamSupport.stream(
        itemsCaptor.getValue().spliterator(), false).toList();
    assertThat(savedItems).extracting(ItineraryItem::getSortOrder)
        .containsExactly(1, 2);
    assertThat(savedItems).extracting(ItineraryItem::getScheduledTime)
        .containsExactly(LocalTime.of(11, 0), LocalTime.of(14, 0));
    assertThat(savedItems).extracting(ItineraryItem::getType)
        .containsExactly(ItineraryItemType.CUSTOM, ItineraryItemType.PLACE);
    assertThat(savedItems.get(1).getTitle()).isEqualTo("해운대 해수욕장");
    assertThat(generation.getStatus()).isEqualTo(AiItineraryGenerationStatus.COMPLETED);
    verify(tripPlanRepository).confirmAiGeneration(1L);
    verify(notificationOutboxService).enqueue(generation,
        AiGenerationNotificationType.COMPLETED);
  }

  @Test
  @DisplayName("공연 고정 시간과 같은 Gemini 일정은 저장하지 않는다")
  void rejectsGeneratedItemAtConcertTime() {
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    ItineraryItem concertItem = ItineraryItem.create(itineraryDay(), 1, LocalTime.of(19, 0),
        ItineraryItemType.CONCERT, null, org.mockito.Mockito.mock(Concert.class), "공연", 120);
    when(generationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(generation));
    when(itineraryItemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(1L))
        .thenReturn(List.of(concertItem));

    assertThatThrownBy(() -> service().complete(10L, input(),
        new GeminiDto.GeneratedItinerary(List.of(
            new GeminiDto.GeneratedItem("19:00", "저녁", 60, null)))))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
    verify(itineraryItemRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("Gemini가 전달하지 않은 장소 후보 ID를 반환하면 저장하지 않는다")
  void rejectsPlaceOutsidePromptCandidates() {
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    when(generationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(generation));
    when(itineraryItemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(1L))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service().complete(10L, inputWithPlaceCandidate(),
        new GeminiDto.GeneratedItinerary(List.of(
            new GeminiDto.GeneratedItem("13:00", "알 수 없는 장소", 60, 3L)))))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
    verify(itineraryItemRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("Gemini 결과에 같은 장소가 중복되면 저장하지 않는다")
  void rejectsDuplicateGeneratedPlace() {
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    when(generationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(generation));
    when(itineraryItemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(1L))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service().complete(10L, inputWithPlaceCandidate(),
        new GeminiDto.GeneratedItinerary(List.of(
            new GeminiDto.GeneratedItem("10:00", "해운대 해수욕장", 60, 2L),
            new GeminiDto.GeneratedItem("14:00", "해운대 해수욕장", 60, 2L)))))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
    verify(itineraryItemRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("기존 일정에 등록된 장소를 Gemini가 다시 반환하면 저장하지 않는다")
  void rejectsGeneratedPlaceAlreadyInItinerary() {
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    Place existingPlace = place(2L);
    ItineraryItem existingItem = ItineraryItem.create(itineraryDay(), 1, LocalTime.of(10, 0),
        ItineraryItemType.PLACE, existingPlace, null, "해운대 해수욕장", 60);
    when(generationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(generation));
    when(itineraryItemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(1L))
        .thenReturn(List.of(existingItem));

    assertThatThrownBy(() -> service().complete(10L, inputWithPlaceCandidate(),
        new GeminiDto.GeneratedItinerary(List.of(
            new GeminiDto.GeneratedItem("14:00", "해운대 해수욕장", 60, 2L)))))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
    verify(itineraryItemRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("retryable Gemini 오류는 다음 시각의 outbox로 예약한다")
  void schedulesRetryableGeminiFailure() {
    RuntimeException exception = new RuntimeException("temporary");
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    ReflectionTestUtils.setField(generation, "attemptCount", 1);
    when(generationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(generation));
    when(retryPolicy.isRetryable(exception)).thenReturn(true);
    when(retryPolicy.canRetry(1)).thenReturn(true);
    when(retryPolicy.nextDelay(1)).thenReturn(Duration.ofSeconds(2));

    service().handleGeminiFailure(10L, exception);

    assertThat(generation.getStatus()).isEqualTo(AiItineraryGenerationStatus.PENDING);
    assertThat(generation.getNextAttemptAt()).isAfter(Instant.now());
    assertThat(generation.getLastFailureReason()).contains("temporary");
    verify(outboxService).enqueueAt(eq(generation), eq(generation.getNextAttemptAt()));
    verify(tripPlanRepository, never()).releaseAiGeneration(any());
    verify(deadLetterRepository, never()).save(any());
    verify(notificationOutboxService, never()).enqueue(any(), any());
  }

  @Test
  @DisplayName("retry 최대 시도에 도달하면 실패·예약 해제·dead-letter를 확정한다")
  void deadLettersExhaustedGeminiFailure() {
    RuntimeException exception = new RuntimeException("still unavailable");
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    ReflectionTestUtils.setField(generation, "attemptCount", 3);
    when(generationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(generation));
    when(retryPolicy.isRetryable(exception)).thenReturn(true);
    when(retryPolicy.canRetry(3)).thenReturn(false);

    service().handleGeminiFailure(10L, exception);

    assertThat(generation.getStatus()).isEqualTo(AiItineraryGenerationStatus.FAILED);
    verify(tripPlanRepository).releaseAiGeneration(1L);
    ArgumentCaptor<AiGenerationDeadLetter> deadLetterCaptor = ArgumentCaptor.forClass(
        AiGenerationDeadLetter.class);
    verify(deadLetterRepository).save(deadLetterCaptor.capture());
    assertThat(deadLetterCaptor.getValue().getGeneration()).isSameAs(generation);
    assertThat(deadLetterCaptor.getValue().getAttemptCount()).isEqualTo(3);
    verify(notificationOutboxService).enqueue(generation, AiGenerationNotificationType.FAILED);
  }

  @Test
  @DisplayName("JSON 같은 non-retryable 오류는 재발행과 dead-letter 없이 실패한다")
  void failsNonRetryableGeminiFailure() {
    RuntimeException exception = new IllegalArgumentException("invalid json");
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    ReflectionTestUtils.setField(generation, "attemptCount", 1);
    when(generationRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(generation));
    when(retryPolicy.isRetryable(exception)).thenReturn(false);

    service().handleGeminiFailure(10L, exception);

    assertThat(generation.getStatus()).isEqualTo(AiItineraryGenerationStatus.FAILED);
    verify(tripPlanRepository).releaseAiGeneration(1L);
    verify(outboxService, never()).enqueueAt(any(), any());
    verify(deadLetterRepository, never()).save(any());
    verify(notificationOutboxService).enqueue(generation, AiGenerationNotificationType.FAILED);
  }

  private AiItineraryGenerationService service() {
    AiGenerationStreamProperties streamProperties = new AiGenerationStreamProperties();
    return new AiItineraryGenerationService(generationRepository, itineraryDayRepository,
        itineraryItemRepository, placeRepository, tripPlanRepository, outboxService,
        streamProperties, retryPolicy, deadLetterRepository, notificationOutboxService);
  }

  private AiItineraryGenerationDto.GenerationInput input() {
    return new AiItineraryGenerationDto.GenerationInput(LocalDate.of(2026, 9, 1),
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        null, List.of(), List.of(), List.of(), List.of());
  }

  private AiItineraryGenerationDto.GenerationInput inputWithPlaceCandidate() {
    return new AiItineraryGenerationDto.GenerationInput(LocalDate.of(2026, 9, 1),
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        null, List.of(), List.of(), List.of(),
        List.of(new AiItineraryGenerationDto.PlaceCandidate(2L, "해운대 해수욕장", "부산")));
  }

  private ItineraryDay itineraryDay() {
    return itineraryDay(false);
  }

  private ItineraryDay itineraryDay(boolean concertDay) {
    TripPlan tripPlan = TripPlan.create(UserFixture.activeUserWithId(1L), null,
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        null, List.of(), List.of());
    ReflectionTestUtils.setField(tripPlan, "id", 1L);
    ItineraryDay itineraryDay = ItineraryDay.create(tripPlan, LocalDate.of(2026, 9, 1), concertDay);
    ReflectionTestUtils.setField(itineraryDay, "id", 1L);
    return itineraryDay;
  }

  private AiItineraryGeneration generation(AiItineraryGenerationStatus status) {
    return generation(status, false);
  }

  private AiItineraryGeneration generation(AiItineraryGenerationStatus status, boolean concertDay) {
    AiItineraryGeneration generation = AiItineraryGeneration.create(itineraryDay(concertDay));
    ReflectionTestUtils.setField(generation, "id", 10L);
    ReflectionTestUtils.setField(generation, "status", status);
    return generation;
  }

  private Place place(Long id) {
    Place place = org.mockito.Mockito.mock(Place.class);
    when(place.getId()).thenReturn(id);
    return place;
  }
}
