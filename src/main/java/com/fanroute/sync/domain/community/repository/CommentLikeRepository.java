package com.fanroute.sync.domain.community.repository;

import com.fanroute.sync.domain.community.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
  boolean existsByCommentIdAndUserId(Long commentId, Long userId);
  long countByCommentId(Long commentId);
  void deleteByCommentIdAndUserId(Long commentId, Long userId);
  void deleteByCommentId(Long commentId);
  void deleteByCommentPostId(Long postId);
}
