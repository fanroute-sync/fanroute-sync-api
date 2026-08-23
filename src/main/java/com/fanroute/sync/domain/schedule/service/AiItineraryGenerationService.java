package com.fanroute.sync.domain.schedule.service;

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
import com.fanroute.sync.domain.schedule.dto.AiItineraryGenerationDto;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGeneration;
import com.fanroute.sync.domain.schedule.entity.AiItineraryGenerationStatus;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.ItineraryItem;
import com.fanroute.sync.domain.schedule.entity.ItineraryItemType;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.AiItineraryGenerationRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryDayRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryItemRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AiItineraryGenerationService {

  private final AiItineraryGenerationRepository generationRepository;
  private final ItineraryDayRepository itineraryDayRepository;
  private final ItineraryItemRepository itineraryItemRepository;
  private final PlaceRepository placeRepository;

  public AiItineraryGenerationDto.CreateResponse request(User user, Long itineraryDayId) {
    ItineraryDay itineraryDay = getOwnedItineraryDay(user, itineraryDayId);
    validateGeneratableDay(itineraryDay);
    AiItineraryGeneration generation = generationRepository.save(
        AiItineraryGeneration.create(itineraryDay));
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

    AiItineraryGeneration retryGeneration = generationRepository.save(
        AiItineraryGeneration.create(generation.getItineraryDay()));
    return AiItineraryGenerationDto.CreateResponse.from(retryGeneration);
  }

  public AiItineraryGenerationDto.StatusResponse cancel(User user, Long generationId) {
    AiItineraryGeneration generation = getOwnedGeneration(user, generationId);
    validateStatus(generation, AiItineraryGenerationStatus.PENDING);
    generation.cancel();
    return AiItineraryGenerationDto.StatusResponse.from(generation);
  }

  public AiItineraryGenerationDto.GenerationInput start(Long generationId) {
    int updated = generationRepository.startIfPending(generationId,
        AiItineraryGenerationStatus.PENDING, AiItineraryGenerationStatus.PROCESSING);
    if (updated == 0) {
      return null;
    }

    AiItineraryGeneration generation = generationRepository.findById(generationId)
        .orElseThrow(() -> new BusinessException(
            ScheduleErrorCode.AI_ITINERARY_GENERATION_NOT_FOUND));
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

  public void complete(Long generationId, AiItineraryGenerationDto.GenerationInput input,
      GeminiDto.GeneratedItinerary generatedItinerary) {
    AiItineraryGeneration generation = generationRepository.findById(generationId)
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
    generation.complete();
  }

  public void fail(Long generationId) {
    generationRepository.findById(generationId).ifPresent(generation -> {
      if (generation.getStatus() == AiItineraryGenerationStatus.PENDING
          || generation.getStatus() == AiItineraryGenerationStatus.PROCESSING) {
        generation.fail();
      }
    });
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
