package com.fanroute.sync.domain.community.entity;

import com.fanroute.sync.domain.user.entity.User;
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
@Table(name = "community_post_likes", uniqueConstraints =
    @UniqueConstraint(columnNames = {"post_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "post_id", nullable = false)
  private Post post;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
  private User user;

  private PostLike(Post post, User user) {
    this.post = post;
    this.user = user;
  }

  public static PostLike create(Post post, User user) {
    return new PostLike(post, user);
  }
}
