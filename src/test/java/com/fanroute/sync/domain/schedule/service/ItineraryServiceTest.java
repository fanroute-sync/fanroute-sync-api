package com.fanroute.sync.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.place.service.PlaceService;
import com.fanroute.sync.domain.schedule.dto.ItineraryDto;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.ItineraryItem;
import com.fanroute.sync.domain.schedule.entity.ItineraryItemType;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.ItineraryDayRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryItemRepository;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class ItineraryServiceTest {

  @Mock
  private ItineraryDayRepository dayRepository;
  @Mock
  private ItineraryItemRepository itemRepository;
  @Mock
  private PlaceService placeService;

  @Test
  @DisplayName("사용자 입력 일정 항목을 다음 순서로 추가한다")
  void addsCustomItem() {
    ItineraryDay day = itineraryDay();
    when(dayRepository.findById(1L)).thenReturn(Optional.of(day));
    when(itemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(1L))
        .thenReturn(List.of());
    when(itemRepository.save(any(ItineraryItem.class))).thenAnswer(invocation -> {
      ItineraryItem item = invocation.getArgument(0);
      ReflectionTestUtils.setField(item, "id", 10L);
      return item;
    });

    ItineraryDto.ItemResponse response = service().addItem(UserFixture.activeUserWithId(1L), 1L,
        new ItineraryDto.CreateItemRequest(ItineraryItemType.CUSTOM, LocalTime.of(13, 0),
            "광안리 산책", 60, null));

    assertThat(response.id()).isEqualTo(10L);
    assertThat(response.sortOrder()).isEqualTo(1);
    assertThat(response.type()).isEqualTo(ItineraryItemType.CUSTOM);
    assertThat(response.fixed()).isFalse();
  }

  @Test
  @DisplayName("공연이 연결된 고정 일정 항목은 수정할 수 없다")
  void rejectsUpdateOfFixedConcertItem() {
    ItineraryItem item = ItineraryItem.create(itineraryDay(), 1, LocalTime.of(19, 0),
        ItineraryItemType.CONCERT, null, org.mockito.Mockito.mock(Concert.class),
        "공연", 120);
    when(itemRepository.findByIdAndItineraryDayTripPlanUserId(10L, 1L))
        .thenReturn(Optional.of(item));

    assertThatThrownBy(() -> service().updateItem(UserFixture.activeUserWithId(1L), 10L,
        new ItineraryDto.UpdateItemRequest(LocalTime.of(20, 0), "공연", 120, null)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.FIXED_ITINERARY_ITEM);
  }

  @Test
  @DisplayName("순서 변경 요청에 중복된 일정 항목이 있으면 거부한다")
  void rejectsReorderWithDuplicateItems() {
    ItineraryDay day = itineraryDay();
    ItineraryItem first = item(day, 10L, 1);
    ItineraryItem second = item(day, 20L, 2);
    when(dayRepository.findById(1L)).thenReturn(Optional.of(day));
    when(itemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(1L))
        .thenReturn(List.of(first, second));

    assertThatThrownBy(() -> service().reorder(UserFixture.activeUserWithId(1L), 1L,
        new ItineraryDto.ReorderRequest(List.of(10L, 10L))))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ScheduleErrorCode.INVALID_ITINERARY_ITEM);
  }

  @Test
  @DisplayName("일정 항목을 재정렬할 때 유니크 제약 충돌 없이 최종 순서를 적용한다")
  void reordersItemsWithTemporarySortOrders() {
    ItineraryDay day = itineraryDay();
    ItineraryItem first = item(day, 10L, 1);
    ItineraryItem second = item(day, 20L, 2);
    when(dayRepository.findById(1L)).thenReturn(Optional.of(day));
    when(itemRepository.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(1L))
        .thenReturn(List.of(first, second));

    service().reorder(UserFixture.activeUserWithId(1L), 1L,
        new ItineraryDto.ReorderRequest(List.of(20L, 10L)));

    assertThat(first.getSortOrder()).isEqualTo(2);
    assertThat(second.getSortOrder()).isEqualTo(1);
    verify(itemRepository).flush();
  }

  private ItineraryService service() {
    return new ItineraryService(dayRepository, itemRepository, placeService);
  }

  private ItineraryDay itineraryDay() {
    TripPlan tripPlan = TripPlan.create(UserFixture.activeUserWithId(1L), null,
        Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-03T09:00:00Z"),
        null, List.of(), List.of());
    ItineraryDay day = ItineraryDay.create(tripPlan, LocalDate.of(2026, 9, 1), false);
    ReflectionTestUtils.setField(day, "id", 1L);
    return day;
  }

  private ItineraryItem item(ItineraryDay day, Long id, int sortOrder) {
    ItineraryItem item = ItineraryItem.create(day, sortOrder, LocalTime.of(13, 0),
        ItineraryItemType.CUSTOM, null, null, "일정", 60);
    ReflectionTestUtils.setField(item, "id", id);
    return item;
  }
}
