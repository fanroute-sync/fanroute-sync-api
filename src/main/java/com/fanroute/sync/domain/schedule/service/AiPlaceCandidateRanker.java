package com.fanroute.sync.domain.schedule.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fanroute.sync.domain.place.entity.Place;
import com.fanroute.sync.domain.place.entity.PlaceTag;
import com.fanroute.sync.domain.schedule.entity.Accommodation;
import com.fanroute.sync.domain.schedule.entity.CompanionType;
import com.fanroute.sync.domain.schedule.entity.TravelMbtiType;

@Component
public class AiPlaceCandidateRanker {

  private static final double DISTANCE_SCORE_RANGE_KM = 20.0;
  private static final int MAX_CANDIDATES = 10;
  private static final Map<TravelMbtiType, Set<PlaceTag>> MBTI_TAGS = Map.of(
      TravelMbtiType.FOOD_EXPLORER, Set.of(PlaceTag.FOOD),
      TravelMbtiType.PHOTO_SENSIBILITY, Set.of(PlaceTag.PHOTO_SPOT, PlaceTag.CAFE, PlaceTag.OCEAN_VIEW),
      TravelMbtiType.HISTORY_CULTURE, Set.of(PlaceTag.INDOOR),
      TravelMbtiType.NATURE_HEALING, Set.of(PlaceTag.PARK, PlaceTag.OCEAN_VIEW),
      TravelMbtiType.SHOPPING_FOCUS, Set.of(PlaceTag.INDOOR),
      TravelMbtiType.CAFE_TOUR, Set.of(PlaceTag.CAFE),
      TravelMbtiType.ACTIVITY, Set.of(PlaceTag.PARK),
      TravelMbtiType.LOCAL_EXPERIENCE, Set.of(PlaceTag.FOOD),
      TravelMbtiType.CONCERT_FOCUS, Set.of(PlaceTag.INDOOR));

  public List<Place> rank(List<Place> places, Set<Long> existingPlaceIds,
      List<Accommodation> accommodations, TravelMbtiType travelMbti, List<CompanionType> companions) {
    Optional<Coordinates> anchor = accommodations.stream()
        .filter(accommodation -> accommodation.getLatitude() != null
            && accommodation.getLongitude() != null)
        .max(Comparator.comparing(Accommodation::getCheckinDate))
        .map(accommodation -> new Coordinates(accommodation.getLatitude(), accommodation.getLongitude()));
    Set<PlaceTag> mbtiTags = travelMbti == null ? Set.of()
        : MBTI_TAGS.getOrDefault(travelMbti, Set.of());
    Set<PlaceTag> companionTags = companionTags(companions);

    return places.stream()
        .filter(place -> !existingPlaceIds.contains(place.getId()))
        .map(place -> scored(place, anchor, mbtiTags, companionTags))
        .sorted(Comparator.comparingInt(ScoredPlace::score).reversed()
            .thenComparing(scored -> scored.place().getName())
            .thenComparing(scored -> scored.place().getId()))
        .limit(MAX_CANDIDATES)
        .map(ScoredPlace::place)
        .toList();
  }

  private ScoredPlace scored(Place place, Optional<Coordinates> anchor, Set<PlaceTag> mbtiTags,
      Set<PlaceTag> companionTags) {
    int distanceScore = 0;
    if (anchor.isPresent()) {
      double distance = distanceKm(anchor.get(), new Coordinates(place.getLatitude(), place.getLongitude()));
      distanceScore = (int) Math.round(Math.max(0,
          (DISTANCE_SCORE_RANGE_KM - distance) / DISTANCE_SCORE_RANGE_KM * 60));
    }
    Set<PlaceTag> tags = place.getTags() == null ? Set.of() : place.getTags();
    int mbtiScore = tags.stream().anyMatch(mbtiTags::contains) ? 25 : 0;
    int companionScore = tags.stream().anyMatch(companionTags::contains) ? 15 : 0;
    return new ScoredPlace(place, distanceScore + mbtiScore + companionScore);
  }

  private Set<PlaceTag> companionTags(List<CompanionType> companions) {
    if (companions == null) {
      return Set.of();
    }
    java.util.HashSet<PlaceTag> tags = new java.util.HashSet<>();
    if (companions.contains(CompanionType.CHILD)) {
      tags.add(PlaceTag.FAMILY);
    }
    if (companions.contains(CompanionType.PARTNER)) {
      tags.add(PlaceTag.DATE);
    }
    return tags;
  }

  private double distanceKm(Coordinates first, Coordinates second) {
    double latitudeDistance = Math.toRadians(second.latitude() - first.latitude());
    double longitudeDistance = Math.toRadians(second.longitude() - first.longitude());
    double haversine = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
        + Math.cos(Math.toRadians(first.latitude())) * Math.cos(Math.toRadians(second.latitude()))
        * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
    return 6371.0 * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
  }

  private record Coordinates(double latitude, double longitude) {
  }

  private record ScoredPlace(Place place, int score) {
  }
}
