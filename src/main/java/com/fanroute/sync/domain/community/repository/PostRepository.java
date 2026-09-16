package com.fanroute.sync.domain.community.repository;

import com.fanroute.sync.domain.community.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
  boolean existsByAuthorIdAndType(Long authorId, com.fanroute.sync.domain.community.entity.PostType type);
}
