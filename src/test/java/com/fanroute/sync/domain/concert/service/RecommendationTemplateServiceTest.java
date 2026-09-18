package com.fanroute.sync.domain.concert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.entity.ConcertSchedule;
import com.fanroute.sync.domain.concert.entity.ConcertTimeSlot;
import com.fanroute.sync.domain.concert.entity.RecommendationTemplate;
import com.fanroute.sync.domain.concert.entity.RecommendationTemplatePlace;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.entity.VenueRecommendedPlace;
import com.fanroute.sync.domain.concert.repository.RecommendationTemplatePlaceRepository;
import com.fanroute.sync.domain.concert.repository.RecommendationTemplateRepository;
import com.fanroute.sync.domain.concert.repository.VenueRecommendedPlaceRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class RecommendationTemplateServiceTest {

  @Mock private RecommendationTemplateRepository templateRepository;
  @Mock private RecommendationTemplatePlaceRepository templatePlaceRepository;
  @Mock private VenueRecommendedPlaceRepository recommendedPlaceRepository;
  @Mock private VenueRepository venueRepository;
  @Mock private ConcertScheduleService concertScheduleService;

  @Test
  @DisplayName("추천 코스도 공연 시간대와 겹치는 장소를 제외한다")
  void filtersTemplatePlacesByConcertTime() {
    Venue venue = venue(1L);
    RecommendationTemplate template = RecommendationTemplate.create(venue, "공연 전 식사", "설명");
    VenueRecommendedPlace lunchPlace = VenueRecommendedPlace.create(
        venue, org.mockito.Mockito.mock(com.fanroute.sync.domain.place.entity.Place.class),
        1, ConcertTimeSlot.LUNCH);
    VenueRecommendedPlace eveningPlace = VenueRecommendedPlace.create(
        venue, org.mockito.Mockito.mock(com.fanroute.sync.domain.place.entity.Place.class),
        2, ConcertTimeSlot.EVENING);
    RecommendationTemplatePlace lunch = RecommendationTemplatePlace.create(template, lunchPlace, 1);
    RecommendationTemplatePlace evening = RecommendationTemplatePlace.create(template, eveningPlace, 2);
    when(templateRepository.findById(1L)).thenReturn(java.util.Optional.of(template));
    when(templatePlaceRepository.findByTemplateIdOrderBySortOrderAsc(1L))
        .thenReturn(List.of(lunch, evening));
    ConcertSchedule schedule = org.mockito.Mockito.mock(ConcertSchedule.class);
    Concert concert = org.mockito.Mockito.mock(Concert.class);
    when(schedule.getConcert()).thenReturn(concert);
    when(concert.getVenue()).thenReturn(venue);
    when(schedule.getPerformanceTime()).thenReturn(LocalTime.of(19, 0));
    when(concertScheduleService.getSchedule(5L)).thenReturn(schedule);

    assertThat(service().getPlaces(1L, 5L)).containsExactly(lunch);
  }

  private RecommendationTemplateService service() {
    return new RecommendationTemplateService(templateRepository, templatePlaceRepository,
        recommendedPlaceRepository, venueRepository, concertScheduleService);
  }

  private Venue venue(Long id) {
    Venue venue = Venue.create("MT10TEST", "테스트 공연장", "부산광역시", 35.1, 129.1);
    ReflectionTestUtils.setField(venue, "id", id);
    return venue;
  }
}
