package com.fanroute.sync.domain.schedule.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.AiGenerationDeadLetterRepository;
import com.fanroute.sync.domain.schedule.repository.AiItineraryGenerationRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryDayRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryItemRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.repository.UserRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AiItineraryGenerationService {

  private final AiItineraryGenerationRepository generationRepository;
  private final ItineraryDayRepository itineraryDayRepository;
  private final ItineraryItemRepository itineraryItemRepository;
  private final PlaceRepository placeRepository;
  private final UserRepository userRepository;
  private final AiGenerationOutboxService outboxService;
  private final AiGenerationStreamProperties streamProperties;
  private final AiGenerationRetryPolicy retryPolicy;
  private final AiGenerationDeadLetterRepository deadLetterRepository;
  private final AiGenerationNotificationOutboxService notificationOutboxService;

  public AiItineraryGenerationDto.CreateResponse request(User user, Long itineraryDayId) {
    ItineraryDay itineraryDay = getOwnedItineraryDay(user, itineraryDayId);
    validateGeneratableDay(itineraryDay);
    reserveAiGeneration(itineraryDay);
    AiItineraryGeneration generation = generationRepository.save(
        AiItineraryGeneration.create(itineraryDay));
    outboxService.enqueue(generation);
    return AiItineraryGenerationDto.CreateResponse.from(generation);
  }

  @Transactional(readOnly = true)
  public AiItineraryGenerationDto.StatusResponse getStatus(User user, Long generationId) {
    AiItineraryGeneration generation = getOwnedGeneration(user, generationId);
    return AiItineraryGenerationDto.StatusResponse.from(generation);
  }

  public AiItineraryGenerationDto.CreateResponse retry(User user, Long generationId) {
    AiItineraryGeneration generation = getOwnedGeneration(user, generationId);
    validateStatus(generation, AiItineraryGenerationStatus.FAILED);
    validateGeneratableDay(generation.getItineraryDay());
    reserveAiGeneration(generation.getItineraryDay());

    AiItineraryGeneration retryGeneration = generationRepository.save(
        AiItineraryGeneration.create(generation.getItineraryDay()));
    outboxService.enqueue(retryGeneration);
    return AiItineraryGenerationDto.CreateResponse.from(retryGeneration);
  }

  public AiItineraryGenerationDto.StatusResponse cancel(User user, Long generationId) {
    AiItineraryGeneration generation = getOwnedGeneration(user, generationId);
    validateStatus(generation, AiItineraryGenerationStatus.PENDING);
    generation.cancel();
    releaseAiGeneration(generation);
    return AiItineraryGenerationDto.StatusResponse.from(generation);
  }

  public AiItineraryGenerationDto.GenerationInput start(Long generationId) {
    Instant now = Instant.now();
    int updated = generationRepository.acquireForProcessing(generationId,
        AiItineraryGenerationStatus.PENDING, AiItineraryGenerationStatus.PROCESSING, now,
        now.plus(streamProperties.getProcessingLease()));
    if (updated == 0) {
      return null;
    }

    AiItineraryGeneration generation = generationRepository.findById(generationId)
        .orElseThrow(() -> new BusinessException(
            ScheduleErrorCode.AI_ITINERARY_GENERATION_NOT_FOUND));
    if (generation.getAttemptCount() == 1 && generation.getCreatedAt() != null) {
      long queueWaitMillis = Math.max(0,
          Duration.between(generation.getCreatedAt(), now).toMillis());
      log.info("AI generation dequeued: generationId={}, attempt={}, queueWaitMs={}",
          generationId, generation.getAttemptCount(), queueWaitMillis);
    }
    ItineraryDay day = generation.getItineraryDay();
    List<ItineraryItem> existingItems = itineraryItemRepository
        .findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(day.getId());
    List<AiItineraryGenerationDto.FixedItem> fixedItems = existingItems.stream()
        .filter(item -> item.getConcert() != null)
        .map(item -> new AiItineraryGenerationDto.FixedItem(item.getScheduledTime(), item.getTitle(),
            item.getDurationMinutes()))
        .toList();
    Set<Long> existingPlaceIds = existingItems.stream()
        .map(ItineraryItem::getPlace)
        .filter(place -> place != null)
        .map(Place::getId)
        .collect(java.util.stream.Collectors.toSet());
    List<AiItineraryGenerationDto.PlaceCandidate> placeCandidates = placeRepository
        .findTop20ByOrderByIdAsc().stream()
        .filter(place -> !existingPlaceIds.contains(place.getId()))
        .map(place -> new AiItineraryGenerationDto.PlaceCandidate(place.getId(), place.getName(),
            place.getAddress()))
        .toList();

    return new AiItineraryGenerationDto.GenerationInput(day.getDate(),
        day.getTripPlan().getArrivalAt(), day.getTripPlan().getDepartureAt(),
        day.getTripPlan().getTravelIntensity(), day.getTripPlan().getCompanions(),
        day.getTripPlan().getPreferences(), fixedItems, placeCandidates);
  }

  @Transactional(readOnly = true)
  public boolean isTerminal(Long generationId) {
    return generationRepository.findById(generationId)
        .map(generation -> generation.getStatus().isTerminal())
        .orElseThrow(() -> new BusinessException(
            ScheduleErrorCode.AI_ITINERARY_GENERATION_NOT_FOUND));
  }

  public void complete(Long generationId, AiItineraryGenerationDto.GenerationInput input,
      GeminiDto.GeneratedItinerary generatedItinerary) {
    AiItineraryGeneration generation = generationRepository.findByIdForUpdate(generationId)
        .orElseThrow(() -> new BusinessException(ScheduleErrorCode.AI_ITINERARY_GENERATION_NOT_FOUND));
    if (generation.getStatus() != AiItineraryGenerationStatus.PROCESSING) {
      return;
    }

    List<GeminiDto.GeneratedItem> generatedItems = validateGeneratedItems(generatedItinerary);
    ItineraryDay day = generation.getItineraryDay();
    List<ItineraryItem> existingItems = itineraryItemRepository
        .findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(day.getId());
    validatePlaceDuplicates(generatedItems, existingItems);
    Map<Long, Place> places = findCandidatePlaces(generatedItems, input.placeCandidates());
    validateConcertConflicts(generatedItems, existingItems);

    int nextSortOrder = existingItems.stream().mapToInt(ItineraryItem::getSortOrder).max()
        .orElse(0) + 1;
    List<GeminiDto.GeneratedItem> sortedItems = generatedItems.stream()
        .sorted(Comparator.comparing(item -> LocalTime.parse(item.scheduledTime())))
        .toList();
    List<ItineraryItem> newItems = IntStream.range(0, sortedItems.size())
        .mapToObj(index -> {
          GeminiDto.GeneratedItem item = sortedItems.get(index);
          return toItineraryItem(day, nextSortOrder + index, item, places.get(item.placeId()));
        })
        .toList();
    itineraryItemRepository.saveAll(newItems);
    confirmAiGeneration(generation);
    generation.complete();
    notificationOutboxService.enqueue(generation, AiGenerationNotificationType.COMPLETED);
  }

  public void fail(Long generationId) {
    fail(generationId, null);
  }

  public void fail(Long generationId, String failureReason) {
    generationRepository.findByIdForUpdate(generationId).ifPresent(generation -> {
      if (generation.getStatus() == AiItineraryGenerationStatus.PENDING
          || generation.getStatus() == AiItineraryGenerationStatus.PROCESSING) {
        generation.fail(normalizeFailureReason(failureReason));
        releaseAiGeneration(generation);
        notificationOutboxService.enqueue(generation, AiGenerationNotificationType.FAILED);
      }
    });
  }

  public void handleGeminiFailure(Long generationId, RuntimeException exception) {
    AiItineraryGeneration generation = generationRepository.findByIdForUpdate(generationId)
        .orElseThrow(() -> new BusinessException(
            ScheduleErrorCode.AI_ITINERARY_GENERATION_NOT_FOUND));
    if (generation.getStatus() != AiItineraryGenerationStatus.PROCESSING) {
      return;
    }

    String failureReason = normalizeFailureReason(
        exception.getClass().getSimpleName() + ": " + exception.getMessage());
    boolean retryable = retryPolicy.isRetryable(exception);
    if (retryable && retryPolicy.canRetry(generation.getAttemptCount())) {
      Instant nextAttemptAt = Instant.now()
          .plus(retryPolicy.nextDelay(generation.getAttemptCount()));
      generation.scheduleRetry(nextAttemptAt, failureReason);
      outboxService.enqueueAt(generation, nextAttemptAt);
      return;
    }

    generation.fail(failureReason);
    releaseAiGeneration(generation);
    if (retryable) {
      deadLetterRepository.save(AiGenerationDeadLetter.create(generation, failureReason));
    }
    notificationOutboxService.enqueue(generation, AiGenerationNotificationType.FAILED);
  }

  private ItineraryDay getOwnedItineraryDay(User user, Long itineraryDayId) {
    return itineraryDayRepository.findById(itineraryDayId)
        .filter(day -> day.getTripPlan().getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new BusinessException(ScheduleErrorCode.ITINERARY_DAY_NOT_FOUND));
  }

  private AiItineraryGeneration getOwnedGeneration(User user, Long generationId) {
    return generationRepository.findByIdAndItineraryDayTripPlanUserId(generationId, user.getId())
        .orElseThrow(() -> new BusinessException(
            ScheduleErrorCode.AI_ITINERARY_GENERATION_NOT_FOUND));
  }

  private void validateGeneratableDay(ItineraryDay itineraryDay) {
    if (itineraryDay.isConcertDay()) {
      throw new BusinessException(
          ScheduleErrorCode.AI_ITINERARY_GENERATION_UNAVAILABLE_ON_CONCERT_DAY);
    }
  }

  private void validateStatus(AiItineraryGeneration generation,
      AiItineraryGenerationStatus expectedStatus) {
    if (generation.getStatus() != expectedStatus) {
      throw new BusinessException(ScheduleErrorCode.INVALID_AI_ITINERARY_GENERATION_STATUS);
    }
  }

  private void reserveAiGeneration(ItineraryDay itineraryDay) {
    int updated = userRepository.reserveAiGeneration(itineraryDay.getTripPlan().getUser().getId(),
        User.AI_GENERATION_LIMIT);
    if (updated == 0) {
      throw new BusinessException(ScheduleErrorCode.AI_ITINERARY_GENERATION_LIMIT_EXCEEDED);
    }
  }

  private void confirmAiGeneration(AiItineraryGeneration generation) {
    int updated = userRepository.confirmAiGeneration(
        generation.getItineraryDay().getTripPlan().getUser().getId());
    if (updated == 0) {
      throw new IllegalStateException("AI generation must have a reserved quota");
    }
  }

  private void releaseAiGeneration(AiItineraryGeneration generation) {
    userRepository.releaseAiGeneration(generation.getItineraryDay().getTripPlan().getUser().getId());
  }

  private String normalizeFailureReason(String failureReason) {
    if (failureReason == null || failureReason.isBlank()) {
      return "Unknown failure";
    }
    return failureReason.length() <= 1000 ? failureReason : failureReason.substring(0, 1000);
  }

  private List<GeminiDto.GeneratedItem> validateGeneratedItems(
      GeminiDto.GeneratedItinerary generatedItinerary) {
    if (generatedItinerary == null || generatedItinerary.items() == null
        || generatedItinerary.items().isEmpty()) {
      throw new BusinessException(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
    }
    for (GeminiDto.GeneratedItem item : generatedItinerary.items()) {
      try {
        LocalTime.parse(item.scheduledTime());
      } catch (RuntimeException exception) {
        throw new BusinessException(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
      }
      if (item.title() == null || item.title().isBlank() || item.title().length() > 200
          || item.durationMinutes() == null || item.durationMinutes() < 1
          || item.durationMinutes() > 720) {
        throw new BusinessException(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
      }
    }
    return generatedItinerary.items();
  }

  private Map<Long, Place> findCandidatePlaces(List<GeminiDto.GeneratedItem> generatedItems,
      List<AiItineraryGenerationDto.PlaceCandidate> placeCandidates) {
    List<Long> placeIds = generatedItems.stream()
        .map(GeminiDto.GeneratedItem::placeId)
        .filter(placeId -> placeId != null)
        .distinct()
        .toList();
    Set<Long> candidateIds = placeCandidates.stream()
        .map(AiItineraryGenerationDto.PlaceCandidate::id)
        .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    if (!candidateIds.containsAll(placeIds)) {
      throw new BusinessException(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
    }
    Map<Long, Place> places = new HashMap<>();
    for (Place place : placeRepository.findAllById(placeIds)) {
      places.put(place.getId(), place);
    }
    if (places.size() != placeIds.size()) {
      throw new BusinessException(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
    }
    return places;
  }

  private void validatePlaceDuplicates(List<GeminiDto.GeneratedItem> generatedItems,
      List<ItineraryItem> existingItems) {
    Set<Long> existingPlaceIds = existingItems.stream()
        .map(ItineraryItem::getPlace)
        .filter(place -> place != null)
        .map(Place::getId)
        .collect(java.util.stream.Collectors.toSet());
    Set<Long> generatedPlaceIds = new HashSet<>();
    for (GeminiDto.GeneratedItem item : generatedItems) {
      Long placeId = item.placeId();
      if (placeId != null && (!generatedPlaceIds.add(placeId)
          || existingPlaceIds.contains(placeId))) {
        throw new BusinessException(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
      }
    }
  }

  private void validateConcertConflicts(List<GeminiDto.GeneratedItem> generatedItems,
      List<ItineraryItem> existingItems) {
    List<LocalTime> concertTimes = existingItems.stream()
        .filter(item -> item.getConcert() != null)
        .map(ItineraryItem::getScheduledTime)
        .toList();
    boolean conflicts = generatedItems.stream()
        .map(item -> LocalTime.parse(item.scheduledTime()))
        .anyMatch(concertTimes::contains);
    if (conflicts) {
      throw new BusinessException(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
    }
  }

  private ItineraryItem toItineraryItem(ItineraryDay day, int sortOrder,
      GeminiDto.GeneratedItem item, Place place) {
    ItineraryItemType type = place == null ? ItineraryItemType.CUSTOM : ItineraryItemType.PLACE;
    String title = place == null ? item.title() : place.getName();
    return ItineraryItem.create(day, sortOrder, LocalTime.parse(item.scheduledTime()), type, place,
        null, title, item.durationMinutes());
  }
}
