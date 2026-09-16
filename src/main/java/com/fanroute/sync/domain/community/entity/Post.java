package com.fanroute.sync.domain.community.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.entity.BaseTimeEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "community_posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PostType type;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String content;

  @ElementCollection
  @CollectionTable(name = "community_post_tags", joinColumns = @JoinColumn(name = "post_id"))
  @Column(name = "tag", nullable = false, length = 50)
  private List<String> tags = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "concert_id")
  private Concert concert;

  @Column(name = "trip_plan_id")
  private Long tripPlanId;

  @Column(name = "companion_date")
  private LocalDate companionDate;

  @Column(name = "capacity")
  private Integer capacity;

  @Column(name = "region", length = 100)
  private String region;

  private Post(User author, PostType type, String title, String content, List<String> tags,
      Concert concert, Long tripPlanId, LocalDate companionDate, Integer capacity,
      String region) {
    this.author = author;
    this.type = type;
    this.title = title;
    this.content = content;
    this.tags = new ArrayList<>(tags);
    this.concert = concert;
    this.tripPlanId = tripPlanId;
    this.companionDate = companionDate;
    this.capacity = capacity;
    this.region = region;
  }

  public static Post create(User author, PostType type, String title, String content,
      List<String> tags, Concert concert, Long tripPlanId, LocalDate companionDate,
      Integer capacity, String region) {
    return new Post(author, type, title, content, tags, concert, tripPlanId,
        companionDate, capacity, region);
  }
}
