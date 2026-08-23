package com.fanroute.sync.domain.concert.entity;

import java.time.LocalTime;

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
@Table(name = "concert_template_items", uniqueConstraints = {
    @UniqueConstraint(name = "uk_concert_template_items_template_id_sort_order", columnNames = {
        "concert_template_id", "sort_order"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcertTemplateItem extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "concert_template_id", nullable = false)
  private ConcertTemplate concertTemplate;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "scheduled_time", nullable = false)
  private LocalTime scheduledTime;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(name = "duration_minutes")
  private Integer durationMinutes;

  private ConcertTemplateItem(ConcertTemplate concertTemplate, int sortOrder,
      LocalTime scheduledTime, String title, Integer durationMinutes) {
    this.concertTemplate = concertTemplate;
    this.sortOrder = sortOrder;
    this.scheduledTime = scheduledTime;
    this.title = title;
    this.durationMinutes = durationMinutes;
  }

  public static ConcertTemplateItem create(ConcertTemplate concertTemplate, int sortOrder,
      LocalTime scheduledTime, String title, Integer durationMinutes) {
    return new ConcertTemplateItem(
        concertTemplate, sortOrder, scheduledTime, title, durationMinutes);
  }
}
