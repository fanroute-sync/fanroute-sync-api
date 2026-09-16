package com.fanroute.sync.domain.community.entity;

import com.fanroute.sync.domain.user.entity.User;
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
@Table(name = "community_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "post_id", nullable = false)
  private Post post;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "author_id", nullable = false)
  private User author;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id")
  private Comment parent;
  @Column(nullable = false, length = 1000)
  private String content;

  private Comment(Post post, User author, Comment parent, String content) {
    this.post = post;
    this.author = author;
    this.parent = parent;
    this.content = content;
  }

  public static Comment create(Post post, User author, Comment parent, String content) {
    return new Comment(post, author, parent, content);
  }
}
