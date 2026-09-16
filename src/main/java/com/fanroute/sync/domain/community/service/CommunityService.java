package com.fanroute.sync.domain.community.service;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.community.dto.CommunityDto;
import com.fanroute.sync.domain.community.entity.Comment;
import com.fanroute.sync.domain.community.entity.CommentLike;
import com.fanroute.sync.domain.community.entity.Post;
import com.fanroute.sync.domain.community.entity.PostLike;
import com.fanroute.sync.domain.community.entity.PostType;
import com.fanroute.sync.domain.community.exception.CommunityErrorCode;
import com.fanroute.sync.domain.community.repository.CommentLikeRepository;
import com.fanroute.sync.domain.community.repository.CommentRepository;
import com.fanroute.sync.domain.community.repository.PostLikeRepository;
import com.fanroute.sync.domain.community.repository.PostRepository;
import com.fanroute.sync.domain.concert.entity.Concert;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.domain.schedule.entity.ItineraryDay;
import com.fanroute.sync.domain.schedule.entity.ItineraryItem;
import com.fanroute.sync.domain.schedule.entity.TripPlan;
import com.fanroute.sync.domain.schedule.exception.ScheduleErrorCode;
import com.fanroute.sync.domain.schedule.repository.ItineraryDayRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryItemRepository;
import com.fanroute.sync.domain.schedule.repository.TripPlanRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {
  private final PostRepository posts;
  private final CommentRepository comments;
  private final PostLikeRepository postLikes;
  private final CommentLikeRepository commentLikes;
  private final TripPlanRepository trips;
  private final ItineraryDayRepository days;
  private final ItineraryItemRepository items;
  private final ConcertRepository concerts;

  public Page<CommunityDto.PostResponse> list(User user, PostType type, String query, String region,
      String sort, int page, int size) {
    if (page < 0 || size < 1 || size > 100 || (!"latest".equals(sort) && !"popular".equals(sort))) {
      throw new BusinessException(CommunityErrorCode.INVALID_POST);
    }
    Specification<Post> spec = (root, criteria, cb) -> { criteria.distinct(true); return cb.conjunction(); };
    if (type != null) {
      spec = spec.and((root, ignored, cb) -> cb.equal(root.get("type"), type));
    }
    if (query != null && !query.isBlank()) {
      String pattern = "%" + query.trim().replaceFirst("^#", "").toLowerCase() + "%";
      spec = spec.and((root, ignored, cb) -> cb.or(
          cb.like(cb.lower(root.get("title")), pattern),
          cb.like(cb.lower(root.join("concert", jakarta.persistence.criteria.JoinType.LEFT).get("title")), pattern),
          cb.like(cb.lower(root.join("concert", jakarta.persistence.criteria.JoinType.LEFT)
              .join("venue", jakarta.persistence.criteria.JoinType.LEFT).get("name")), pattern),
          cb.like(cb.lower(root.join("tags", jakarta.persistence.criteria.JoinType.LEFT)), pattern)));
    }
    if (region != null && !region.isBlank()) {
      spec = spec.and((root, ignored, cb) -> cb.like(cb.lower(root.get("region")),
          "%" + region.trim().toLowerCase() + "%"));
    }
    if ("popular".equals(sort)) {
      List<CommunityDto.PostResponse> all = posts.findAll(spec).stream()
          .map(post -> toPost(user, post))
          .sorted((a, b) -> {
            int likes = Long.compare(b.likeCount(), a.likeCount());
            return likes != 0 ? likes : b.createdAt().compareTo(a.createdAt());
          }).toList();
      return new PageImpl<>(all.stream().skip((long) page * size).limit(size).toList(),
          PageRequest.of(page, size), all.size());
    }
    return posts.findAll(spec, PageRequest.of(page, size,
        Sort.by(Sort.Direction.DESC, "createdAt", "id"))).map(post -> toPost(user, post));
  }

  public CommunityDto.PostDetailResponse detail(User user, Long id) {
    Post post = findPost(id);
    return new CommunityDto.PostDetailResponse(toPost(user, post),
        comments.findByPostIdOrderByCreatedAtAscIdAsc(id).stream()
            .map(comment -> toComment(user, comment)).toList());
  }

  @Transactional
  public CommunityDto.PostResponse create(User user, CommunityDto.CreatePostRequest request) {
    if (request.type() == null) throw new BusinessException(CommunityErrorCode.INVALID_POST);
    Concert concert = null;
    Long tripId = null;
    String content = request.content();
    if (request.type() == PostType.ROUTE) {
      if (request.tripPlanId() == null) throw new BusinessException(CommunityErrorCode.INVALID_POST);
      TripPlan trip = trips.findByIdAndUserId(request.tripPlanId(), user.getId())
          .orElseThrow(() -> new BusinessException(ScheduleErrorCode.TRIP_PLAN_NOT_FOUND));
      tripId = trip.getId();
      concert = trip.getConcert();
      content = routeText(trip);
    } else if (request.type() == PostType.COMPANION) {
      if (request.concertId() == null || request.companionDate() == null
          || request.capacity() == null || request.capacity() < 2) {
        throw new BusinessException(CommunityErrorCode.INVALID_POST);
      }
      if (posts.existsByAuthorIdAndType(user.getId(), PostType.COMPANION)) {
        throw new BusinessException(CommunityErrorCode.ACTIVE_COMPANION_EXISTS);
      }
      concert = concerts.findById(request.concertId())
          .orElseThrow(() -> new BusinessException(ConcertErrorCode.CONCERT_NOT_FOUND));
    } else if (content == null || content.isBlank()) {
      throw new BusinessException(CommunityErrorCode.INVALID_POST);
    }
    List<String> tags = request.tags() == null ? List.of() : request.tags().stream()
        .filter(Objects::nonNull).map(String::trim).filter(t -> !t.isBlank()).toList();
    String region = request.region();
    if (region == null && concert != null) region = concert.getVenue().getAddress();
    return toPost(user, posts.save(Post.create(user, request.type(), request.title().trim(), content,
        tags, concert, tripId, request.companionDate(), request.capacity(), region)));
  }

  @Transactional
  public void deletePost(User user, Long id) {
    Post post = findPost(id);
    requireAuthor(user, post.getAuthor().getId());
    commentLikes.deleteByCommentPostId(id);
    comments.deleteByPostIdAndParentIsNotNull(id);
    comments.flush();
    comments.deleteByPostId(id);
    postLikes.deleteByPostId(id);
    posts.delete(post);
  }

  @Transactional
  public void likePost(User user, Long id) {
    Post post = findPost(id);
    if (!postLikes.existsByPostIdAndUserId(id, user.getId())) {
      postLikes.save(PostLike.create(post, user));
    }
  }

  @Transactional
  public void unlikePost(User user, Long id) {
    findPost(id);
    postLikes.deleteByPostIdAndUserId(id, user.getId());
  }

  @Transactional
  public CommunityDto.CommentResponse createComment(User user, Long postId,
      CommunityDto.CreateCommentRequest request) {
    Post post = findPost(postId);
    Comment parent = null;
    if (request.parentId() != null) {
      parent = findComment(postId, request.parentId());
      if (parent.getParent() != null) parent = parent.getParent();
    }
    return toComment(user, comments.save(Comment.create(post, user, parent,
        request.content().trim())));
  }

  @Transactional
  public void deleteComment(User user, Long postId, Long commentId) {
    Comment comment = findComment(postId, commentId);
    requireAuthor(user, comment.getAuthor().getId());
    if (comment.getParent() == null) {
      for (Comment reply : comments.findByParentId(commentId)) {
        commentLikes.deleteByCommentId(reply.getId());
      }
      comments.deleteByParentId(commentId);
      comments.flush();
    }
    commentLikes.deleteByCommentId(commentId);
    comments.delete(comment);
  }

  @Transactional
  public void likeComment(User user, Long postId, Long commentId) {
    Comment comment = findComment(postId, commentId);
    if (!commentLikes.existsByCommentIdAndUserId(commentId, user.getId())) {
      commentLikes.save(CommentLike.create(comment, user));
    }
  }

  @Transactional
  public void unlikeComment(User user, Long postId, Long commentId) {
    findComment(postId, commentId);
    commentLikes.deleteByCommentIdAndUserId(commentId, user.getId());
  }

  private Post findPost(Long id) {
    return posts.findById(id)
        .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
  }

  private Comment findComment(Long postId, Long id) {
    Comment comment = comments.findById(id)
        .orElseThrow(() -> new BusinessException(CommunityErrorCode.COMMENT_NOT_FOUND));
    if (!comment.getPost().getId().equals(postId)) {
      throw new BusinessException(CommunityErrorCode.COMMENT_NOT_FOUND);
    }
    return comment;
  }

  private void requireAuthor(User user, Long authorId) {
    if (!user.getId().equals(authorId)) throw new BusinessException(CommunityErrorCode.FORBIDDEN);
  }

  private CommunityDto.PostResponse toPost(User user, Post post) {
    Concert concert = post.getConcert();
    return new CommunityDto.PostResponse(post.getId(), post.getType(), post.getTitle(),
        post.getContent(), post.getTags(), post.getAuthor().getId(),
        post.getAuthor().getNickname(), concert == null ? null : concert.getId(),
        concert == null ? null : concert.getTitle(), post.getTripPlanId(),
        post.getCompanionDate(), post.getCapacity(),
        post.getType() == PostType.COMPANION ? 1 : null, post.getRegion(),
        postLikes.countByPostId(post.getId()),
        postLikes.existsByPostIdAndUserId(post.getId(), user.getId()),
        comments.countByPostId(post.getId()),
        post.getCreatedAt());
  }

  private CommunityDto.CommentResponse toComment(User user, Comment comment) {
    return new CommunityDto.CommentResponse(comment.getId(),
        comment.getParent() == null ? null : comment.getParent().getId(),
        comment.getAuthor().getId(), comment.getAuthor().getNickname(),
        comment.getContent(), commentLikes.countByCommentId(comment.getId()),
        commentLikes.existsByCommentIdAndUserId(comment.getId(), user.getId()),
        comment.getCreatedAt());
  }

  private String routeText(TripPlan trip) {
    StringBuilder text = new StringBuilder();
    for (ItineraryDay day : days.findByTripPlanIdOrderByDateAsc(trip.getId())) {
      text.append(day.getDate()).append(System.lineSeparator());
      for (ItineraryItem item : items.findByItineraryDayIdOrderByScheduledTimeAscSortOrderAsc(day.getId())) {
        text.append(item.getScheduledTime() == null ? "" : item.getScheduledTime() + " ")
            .append(item.getTitle() != null ? item.getTitle() : item.getPlace() != null ? item.getPlace().getName() : item.getConcert() != null ? item.getConcert().getTitle() : "")
            .append(System.lineSeparator());
      }
    }
    return text.toString().trim();
  }
}
