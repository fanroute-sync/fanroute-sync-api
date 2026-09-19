package com.fanroute.sync.domain.schedule.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.VenueItineraryTemplate;
import com.fanroute.sync.domain.concert.entity.VenueItineraryTemplateItem;
import com.fanroute.sync.domain.concert.service.VenueItineraryTemplateService;
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
  private final VenueItineraryTemplateService templateService;

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
    int nextOrder = itemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(dayId)
        .stream()
        .mapToInt(ItineraryItem::getSortOrder)
        .max()
        .orElse(0) + 1;
    ItineraryItem item = itemRepository.save(ItineraryItem.create(day, nextOrder, request.scheduledTime(),
        request.type(), place, null, request.title(), request.durationMinutes()));
    return toItemResponse(item);
  }

  /** 컬렉션에서 선택한 장소를 사용자가 자기 일정에 넣습니다. */
  public ItineraryDto.ItemResponse addPlace(
      User user, Long dayId, ItineraryDto.AddPlaceRequest request) {
    ItineraryDay day = getOwnedDay(user, dayId);
    Place place = placeService.getPlace(request.placeId());

    int nextOrder = itemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(dayId)
        .stream()
        .mapToInt(ItineraryItem::getSortOrder)
        .max()
        .orElse(0) + 1;
    ItineraryItem item = itemRepository.save(ItineraryItem.create(day, nextOrder,
        request.scheduledTime(), ItineraryItemType.PLACE, place, null,
        place.getName(), null));
    return toItemResponse(item);
  }

  public List<ItineraryDto.ItemResponse> addTemplate(
      User user, Long dayId, ItineraryDto.AddTemplateRequest request) {
    ItineraryDay day = getOwnedDay(user, dayId);
    VenueItineraryTemplate template = templateService.getTemplate(request.templateId());
    Concert concert = day.getTripPlan().getConcert();
    if (concert == null || !concert.getVenue().getId().equals(template.getVenue().getId())) {
      throw new BusinessException(ScheduleErrorCode.ITINERARY_TEMPLATE_VENUE_MISMATCH);
    }

    int nextOrder = itemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(dayId)
        .stream().mapToInt(ItineraryItem::getSortOrder).max().orElse(0) + 1;
    List<VenueItineraryTemplateItem> templateItems = templateService.getItems(template.getId());
    List<ItineraryItem> items = new ArrayList<>();
    for (VenueItineraryTemplateItem templateItem : templateItems) {
      items.add(ItineraryItem.create(day, nextOrder++, templateItem.getDefaultTime(),
          ItineraryItemType.PLACE, templateItem.getPlace(), null,
          templateItem.getPlace().getName(), templateItem.getDefaultDurationMinutes()));
    }
    return itemRepository.saveAll(items).stream().map(this::toItemResponse).toList();
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
    getOwnedDay(user, dayId);
    List<ItineraryItem> items = itemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(dayId);
    Set<Long> requestedIds = new HashSet<>(request.itemIds());
    Set<Long> itemIds = items.stream().map(ItineraryItem::getId).collect(Collectors.toSet());
    if (requestedIds.size() != items.size() || !requestedIds.equals(itemIds)) {
      throw new BusinessException(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
    }

    Map<Long, ItineraryItem> itemsById = items.stream()
        .collect(Collectors.toMap(ItineraryItem::getId, Function.identity()));
    for (int index = 0; index < request.itemIds().size(); index++) {
      Long itemId = request.itemIds().get(index);
      itemsById.get(itemId).changeSortOrder(-(index + 1));
    }
    itemRepository.flush();
    for (int index = 0; index < request.itemIds().size(); index++) {
      Long itemId = request.itemIds().get(index);
      itemsById.get(itemId).changeSortOrder(index + 1);
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
    if ((type == ItineraryItemType.PLACE && placeId == null)
        || (type == ItineraryItemType.CUSTOM && placeId != null)) {
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
