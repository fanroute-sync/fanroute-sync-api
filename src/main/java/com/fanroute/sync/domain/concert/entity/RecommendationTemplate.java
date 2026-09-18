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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "recommendation_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationTemplate extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venue_id", nullable = false)
  private Venue venue;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  private RecommendationTemplate(Venue venue, String name, String description) {
    this.venue = venue;
    this.name = name;
    this.description = description;
  }

  public static RecommendationTemplate create(Venue venue, String name, String description) {
    return new RecommendationTemplate(venue, name, description);
  }

  public void update(String name, String description) {
    this.name = name;
    this.description = description;
  }
}
