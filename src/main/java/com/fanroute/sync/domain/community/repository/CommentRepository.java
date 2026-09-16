package com.fanroute.sync.domain.community.repository;

import java.util.List;
import com.fanroute.sync.domain.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
  List<Comment> findByPostIdOrderByCreatedAtAscIdAsc(Long postId);
  long countByPostId(Long postId);
  List<Comment> findByParentId(Long parentId);
  void deleteByParentId(Long parentId);
  void deleteByPostIdAndParentIsNotNull(Long postId);
  void deleteByPostId(Long postId);
}
