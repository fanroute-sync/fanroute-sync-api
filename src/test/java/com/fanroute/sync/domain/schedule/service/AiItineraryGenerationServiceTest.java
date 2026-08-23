package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.repository.PlaceRepository;
import com.fanroute.sync.domain.schedule.client.GeminiDto;
import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.ItineraryItem;
import com.fanroute.sync.domain.schedule.entity.ItineraryItemType;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.AiItineraryGenerationRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryDayRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryItemRepository;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class AiItineraryGenerationServiceTest {

  @Mock
  private AiItineraryGenerationRepository generationRepository;
  @Mock
  private ItineraryDayRepository itineraryDayRepository;
  @Mock
  private ItineraryItemRepository itineraryItemRepository;
  @Mock
  private PlaceRepository placeRepository;

  @Test
  @DisplayName("내 날짜별 일정에 PENDING AI 생성 작업을 만든다")
  void requestsGeneration() {
    ItineraryDay itineraryDay = itineraryDay();
    when(itineraryDayRepository.findById(1L)).thenReturn(Optional.of(itineraryDay));
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
  void startsPendingGenerationAndBuildsInput() {
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    ItineraryItem concertItem = ItineraryItem.create(itineraryDay(), 1, LocalTime.of(19, 0),
        ItineraryItemType.CONCERT, null, org.mockito.Mockito.mock(Concert.class), "공연", 120);
    when(generationRepository.startIfPending(10L, AiItineraryGenerationStatus.PENDING,
        AiItineraryGenerationStatus.PROCESSING)).thenReturn(1);
    when(generationRepository.findById(10L)).thenReturn(Optional.of(generation));
    when(itineraryItemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(1L))
        .thenReturn(List.of(concertItem));
    when(placeRepository.findTop20ByOrderByIdAsc()).thenReturn(List.of());

    AiItineraryGenerationDto.GenerationInput input = service().start(10L);

    assertThat(input.date()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(input.fixedItems()).singleElement()
        .extracting(AiItineraryGenerationDto.FixedItem::scheduledTime)
        .isEqualTo(LocalTime.of(19, 0));
  }

  @Test
  @DisplayName("이미 처리된 작업은 다시 시작하지 않는다")
  void doesNotStartAlreadyClaimedGeneration() {
    when(generationRepository.startIfPending(10L, AiItineraryGenerationStatus.PENDING,
        AiItineraryGenerationStatus.PROCESSING)).thenReturn(0);

    assertThat(service().start(10L)).isNull();
    verify(generationRepository, never()).findById(any());
  }

  @Test
  @DisplayName("검증된 Gemini 결과를 장소 참조와 함께 일반 일정으로 저장한다")
  void savesGeneratedItineraryItems() {
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    Place place = org.mockito.Mockito.mock(Place.class);
    when(place.getId()).thenReturn(2L);
    when(place.getName()).thenReturn("해운대 해수욕장");
    when(generationRepository.findById(10L)).thenReturn(Optional.of(generation));
    when(itineraryItemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(1L))
        .thenReturn(List.of());
    when(placeRepository.findAllById(List.of(2L))).thenReturn(List.of(place));

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
  }

  @Test
  @DisplayName("공연 고정 시간과 같은 Gemini 일정은 저장하지 않는다")
  void rejectsGeneratedItemAtConcertTime() {
    AiItineraryGeneration generation = generation(AiItineraryGenerationStatus.PROCESSING);
    ItineraryItem concertItem = ItineraryItem.create(itineraryDay(), 1, LocalTime.of(19, 0),
        ItineraryItemType.CONCERT, null, org.mockito.Mockito.mock(Concert.class), "공연", 120);
    when(generationRepository.findById(10L)).thenReturn(Optional.of(generation));
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
    when(generationRepository.findById(10L)).thenReturn(Optional.of(generation));
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

  private AiItineraryGenerationService service() {
    return new AiItineraryGenerationService(generationRepository, itineraryDayRepository,
        itineraryItemRepository, placeRepository);
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
    TripPlan tripPlan = TripPlan.create(UserFixture.activeUserWithId(1L), null,
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        null, List.of(), List.of());
    ItineraryDay itineraryDay = ItineraryDay.create(tripPlan, LocalDate.of(2026, 9, 1), false);
    ReflectionTestUtils.setField(itineraryDay, "id", 1L);
    return itineraryDay;
  }

  private AiItineraryGeneration generation(AiItineraryGenerationStatus status) {
    AiItineraryGeneration generation = AiItineraryGeneration.create(itineraryDay());
    ReflectionTestUtils.setField(generation, "id", 10L);
    ReflectionTestUtils.setField(generation, "status", status);
    return generation;
  }
}
