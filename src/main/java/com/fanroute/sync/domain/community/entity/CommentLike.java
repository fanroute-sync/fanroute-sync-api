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
@Table(name = "community_comment_likes", uniqueConstraints =
    @UniqueConstraint(columnNames = {"comment_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "comment_id", nullable = false)
  private Comment comment;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
  private User user;

  private CommentLike(Comment comment, User user) {
    this.comment = comment;
    this.user = user;
  }

  public static CommentLike create(Comment comment, User user) {
    return new CommentLike(comment, user);
  }
}
