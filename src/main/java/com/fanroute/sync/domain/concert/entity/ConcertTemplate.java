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
@Table(name = "concert_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcertTemplate extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "concert_id", nullable = false)
  private Concert concert;

  @Column(length = 255)
  private String summary;

  @Column(columnDefinition = "TEXT")
  private String description;

  private ConcertTemplate(Concert concert, String summary, String description) {
    this.concert = concert;
    this.summary = summary;
    this.description = description;
  }

  public static ConcertTemplate create(Concert concert, String summary, String description) {
    return new ConcertTemplate(concert, summary, description);
  }
}
