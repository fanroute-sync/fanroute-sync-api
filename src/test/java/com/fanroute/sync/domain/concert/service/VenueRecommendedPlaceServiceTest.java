package com.fanroute.sync.domain.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.concert.dto.VenueRecommendedPlaceDto;
import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.entity.ConcertTimeSlot;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.entity.VenueRecommendedPlace;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.VenueRecommendedPlaceRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;
import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.service.PlaceService;
import com.fanroute.sync.global.common.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class VenueRecommendedPlaceServiceTest {

  @Mock private VenueRecommendedPlaceRepository repository;
  @Mock private VenueRepository venueRepository;
  @Mock private PlaceService placeService;
  @Mock private ConcertScheduleService concertScheduleService;

  @Test
  @DisplayName("관리자가 공연장 추천 장소를 등록한다")
  void createsRecommendation() {
    Venue venue = venue(1L);
    Place place = org.mockito.Mockito.mock(Place.class);
    when(venueRepository.findById(1L)).thenReturn(Optional.of(venue));
    when(placeService.getPlace(2L)).thenReturn(place);
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    VenueRecommendedPlace result = service().create(
        new VenueRecommendedPlaceDto.CreateRequest(1L, 2L, 1, ConcertTimeSlot.LUNCH));

    assertThat(result.getVenue()).isEqualTo(venue);
    assertThat(result.getPlace()).isEqualTo(place);
    assertThat(result.getRecommendedTimeSlot()).isEqualTo(ConcertTimeSlot.LUNCH);
  }

  @Test
  @DisplayName("공연 시간대와 겹치는 추천을 제외하고 가까운 시간대순으로 정렬한다")
  void filtersAndSortsByConcertTime() {
    Venue venue = venue(1L);
    Place place = org.mockito.Mockito.mock(Place.class);
    VenueRecommendedPlace morning =
        VenueRecommendedPlace.create(venue, place, 1, ConcertTimeSlot.MORNING);
    VenueRecommendedPlace lunch =
        VenueRecommendedPlace.create(venue, place, 2, ConcertTimeSlot.LUNCH);
    VenueRecommendedPlace evening =
        VenueRecommendedPlace.create(venue, place, 3, ConcertTimeSlot.EVENING);
    when(repository.findByVenueIdOrderBySortOrderAsc(1L))
        .thenReturn(List.of(morning, lunch, evening));
    ConcertSchedule schedule = scheduleAt(venue, LocalTime.of(19, 0));
    when(concertScheduleService.getSchedule(5L)).thenReturn(schedule);

    List<VenueRecommendedPlace> result = service().getRecommendations(1L, 5L);

    assertThat(result).containsExactly(lunch, morning);
  }

  @Test
  @DisplayName("같은 공연장의 장소 또는 순서가 중복되면 충돌 오류로 변환한다")
  void convertsConstraintViolation() {
    when(venueRepository.findById(1L)).thenReturn(Optional.of(venue(1L)));
    when(placeService.getPlace(2L)).thenReturn(org.mockito.Mockito.mock(Place.class));
    when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

    assertThatThrownBy(() -> service().create(
        new VenueRecommendedPlaceDto.CreateRequest(1L, 2L, 1, null)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ConcertErrorCode.DUPLICATE_RECOMMENDED_PLACE);
  }

  private VenueRecommendedPlaceService service() {
    return new VenueRecommendedPlaceService(
        repository, venueRepository, placeService, concertScheduleService);
  }

  private Venue venue(Long id) {
    Venue venue = Venue.create("MT10TEST", "테스트 공연장", "부산광역시", 35.1, 129.1);
    ReflectionTestUtils.setField(venue, "id", id);
    return venue;
  }

  private ConcertSchedule scheduleAt(Venue venue, LocalTime performanceTime) {
    ConcertSchedule schedule = org.mockito.Mockito.mock(ConcertSchedule.class);
    Concert concert = org.mockito.Mockito.mock(Concert.class);
    when(schedule.getConcert()).thenReturn(concert);
    when(concert.getVenue()).thenReturn(venue);
    when(schedule.getPerformanceTime()).thenReturn(performanceTime);
    return schedule;
  }
}
