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

@Component
public class AiPlaceCandidateRanker {

  private static final double DISTANCE_SCORE_RANGE_KM = 20.0;
  private static final int MAX_CANDIDATES = 10;
  private static final Map<String, Set<PlaceTag>> MBTI_TAGS = Map.of(
      "맛집탐방형", Set.of(PlaceTag.FOOD),
      "감성사진형", Set.of(PlaceTag.PHOTO_SPOT, PlaceTag.CAFE, PlaceTag.OCEAN_VIEW),
      "역사문화형", Set.of(PlaceTag.INDOOR),
      "자연힐링형", Set.of(PlaceTag.PARK, PlaceTag.OCEAN_VIEW),
      "쇼핑집중형", Set.of(PlaceTag.INDOOR),
      "카페투어형", Set.of(PlaceTag.CAFE),
      "액티비티형", Set.of(PlaceTag.PARK),
      "로컬체험형", Set.of(PlaceTag.FOOD),
      "공연몰입형", Set.of(PlaceTag.INDOOR));

  public List<Place> rank(List<Place> places, Set<Long> existingPlaceIds,
      List<Accommodation> accommodations, String travelMbti, List<String> companions) {
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

  private Set<PlaceTag> companionTags(List<String> companions) {
    if (companions == null) {
      return Set.of();
    }
    java.util.HashSet<PlaceTag> tags = new java.util.HashSet<>();
    if (companions.contains("아이")) {
      tags.add(PlaceTag.FAMILY);
    }
    if (companions.contains("연인")) {
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
