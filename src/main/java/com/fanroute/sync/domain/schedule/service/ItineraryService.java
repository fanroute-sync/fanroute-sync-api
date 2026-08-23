package com.fanroute.sync.domain.schedule.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.service.PlaceService;
import com.fanroute.sync.domain.schedule.dto.ItineraryDto;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.ItineraryItem;
import com.fanroute.sync.domain.schedule.entity.ItineraryItemType;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.ItineraryDayRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryItemRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ItineraryService {
  private final ItineraryDayRepository dayRepository;
  private final ItineraryItemRepository itemRepository;
  private final PlaceService placeService;

  @Transactional(readOnly = true)
  public ItineraryDto.DayResponse getDay(User user, Long tripPlanId, LocalDate date) {
    ItineraryDay day = getOwnedDay(user, tripPlanId, date);
    return toDayResponse(day);
  }

  public ItineraryDto.ItemResponse addItem(User user, Long dayId, ItineraryDto.CreateItemRequest request) {
    ItineraryDay day = getOwnedDay(user, dayId);
    if (request.type() == ItineraryItemType.CONCERT) {
      throw new BusinessException(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
    }
    Place place = validatePlace(request.type(), request.placeId());
    int nextOrder = itemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(dayId).size() + 1;
    ItineraryItem item = itemRepository.save(ItineraryItem.create(day, nextOrder, request.scheduledTime(),
        request.type(), place, null, request.title(), request.durationMinutes()));
    return toItemResponse(item);
  }

  public ItineraryDto.ItemResponse updateItem(User user, Long itemId, ItineraryDto.UpdateItemRequest request) {
    ItineraryItem item = getOwnedItem(user, itemId);
    ensureEditable(item);
    item.update(request.scheduledTime(), request.title(), request.durationMinutes(),
        validatePlace(item.getType(), request.placeId()));
    return toItemResponse(item);
  }

  public void deleteItem(User user, Long itemId) {
    ItineraryItem item = getOwnedItem(user, itemId);
    ensureEditable(item);
    itemRepository.delete(item);
  }

  public void reorder(User user, Long dayId, ItineraryDto.ReorderRequest request) {
    ItineraryDay day = getOwnedDay(user, dayId);
    List<ItineraryItem> items = itemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(dayId);
    Set<Long> requestedIds = new HashSet<>(request.itemIds());
    if (requestedIds.size() != items.size() || !requestedIds.equals(items.stream().map(ItineraryItem::getId).collect(java.util.stream.Collectors.toSet()))) {
      throw new BusinessException(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
    }
    for (int index = 0; index < request.itemIds().size(); index++) {
      Long itemId = request.itemIds().get(index);
      items.stream().filter(item -> item.getId().equals(itemId)).findFirst()
          .orElseThrow().changeSortOrder(index + 1);
    }
  }

  private ItineraryDay getOwnedDay(User user, Long tripPlanId, LocalDate date) {
    return dayRepository.findByTripPlanIdAndDate(tripPlanId, date)
        .filter(day -> day.getTripPlan().getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new BusinessException(ScheduleErrorCode.ITINERARY_DAY_NOT_FOUND));
  }
  private ItineraryDay getOwnedDay(User user, Long dayId) {
    return dayRepository.findById(dayId)
        .filter(day -> day.getTripPlan().getUser().getId().equals(user.getId()))
        .orElseThrow(() -> new BusinessException(ScheduleErrorCode.ITINERARY_DAY_NOT_FOUND));
  }
  private ItineraryItem getOwnedItem(User user, Long itemId) {
    return itemRepository.findByIdAndItineraryDayTripPlanUserId(itemId, user.getId())
        .orElseThrow(() -> new BusinessException(ScheduleErrorCode.ITINERARY_ITEM_NOT_FOUND));
  }
  private Place validatePlace(ItineraryItemType type, Long placeId) {
    if (type == ItineraryItemType.PLACE && placeId == null || type == ItineraryItemType.CUSTOM && placeId != null) {
      throw new BusinessException(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
    }
    return placeId == null ? null : placeService.getPlace(placeId);
  }
  private void ensureEditable(ItineraryItem item) {
    if (item.getConcert() != null) throw new BusinessException(ScheduleErrorCode.FIXED_ITINERARY_ITEM);
  }
  private ItineraryDto.DayResponse toDayResponse(ItineraryDay day) {
    return new ItineraryDto.DayResponse(day.getId(), day.getDate(), day.isConcertDay(),
        itemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(day.getId()).stream().map(this::toItemResponse).toList());
  }
  private ItineraryDto.ItemResponse toItemResponse(ItineraryItem item) {
    return new ItineraryDto.ItemResponse(item.getId(), item.getSortOrder(), item.getScheduledTime(), item.getType(), item.getTitle(), item.getDurationMinutes(), item.getPlace() == null ? null : item.getPlace().getId(), item.getConcert() == null ? null : item.getConcert().getId(), item.getConcert() != null);
  }
}
