package com.fanroute.sync.domain.community.repository;

import com.fanroute.sync.domain.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
  boolean existsByPostIdAndUserId(Long postId, Long userId);
  long countByPostId(Long postId);
  void deleteByPostIdAndUserId(Long postId, Long userId);
  void deleteByPostId(Long postId);
}
