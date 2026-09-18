package com.fanroute.sync.domain.concert.entity;

import com.fanroute.sync.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "recommendation_template_places", uniqueConstraints = {
    @UniqueConstraint(name = "uk_template_places_template_recommendation",
        columnNames = {"template_id", "venue_recommended_place_id"}),
    @UniqueConstraint(name = "uk_template_places_template_sort_order",
        columnNames = {"template_id", "sort_order"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationTemplatePlace extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", nullable = false)
  private RecommendationTemplate template;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venue_recommended_place_id", nullable = false)
  private VenueRecommendedPlace recommendedPlace;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  private RecommendationTemplatePlace(
      RecommendationTemplate template, VenueRecommendedPlace recommendedPlace, int sortOrder) {
    this.template = template;
    this.recommendedPlace = recommendedPlace;
    this.sortOrder = sortOrder;
  }

  public static RecommendationTemplatePlace create(
      RecommendationTemplate template, VenueRecommendedPlace recommendedPlace, int sortOrder) {
    return new RecommendationTemplatePlace(template, recommendedPlace, sortOrder);
  }
}
