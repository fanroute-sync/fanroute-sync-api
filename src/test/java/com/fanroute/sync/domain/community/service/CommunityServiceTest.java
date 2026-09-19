package com.fanroute.sync.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.community.dto.CommunityDto;
import com.fanroute.sync.domain.chat.service.ChatService;
import com.fanroute.sync.domain.community.entity.Comment;
import com.fanroute.sync.domain.community.entity.Post;
import com.fanroute.sync.domain.community.entity.PostType;
import com.fanroute.sync.domain.community.exception.CommunityErrorCode;
import com.fanroute.sync.domain.community.repository.CommentLikeRepository;
import com.fanroute.sync.domain.community.repository.CommentRepository;
import com.fanroute.sync.domain.community.repository.PostLikeRepository;
import com.fanroute.sync.domain.community.repository.PostRepository;
import com.fanroute.sync.domain.concert.repository.ConcertRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryDayRepository;
import com.fanroute.sync.domain.schedule.repository.ItineraryItemRepository;
import com.fanroute.sync.domain.schedule.repository.TripPlanRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.exception.BusinessException;
import com.fanroute.sync.support.UserFixture;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {
  @Mock PostRepository posts;
  @Mock CommentRepository comments;
  @Mock PostLikeRepository postLikes;
  @Mock CommentLikeRepository commentLikes;
  @Mock TripPlanRepository trips;
  @Mock ItineraryDayRepository days;
  @Mock ItineraryItemRepository items;
  @Mock ConcertRepository concerts;
  @Mock ChatService chatService;

  @Test
  void detailIncludesCurrentUsersPostAndCommentLikes() {
    User user = UserFixture.activeUser();
    ReflectionTestUtils.setField(user, "id", 1L);
    Post post = Post.create(user, PostType.INFO, "title", "body", List.of(),
        null, null, null, null, null);
    ReflectionTestUtils.setField(post, "id", 10L);
    Comment comment = Comment.create(post, user, null, "comment");
    ReflectionTestUtils.setField(comment, "id", 20L);
    when(posts.findById(10L)).thenReturn(Optional.of(post));
    when(comments.findByPostIdOrderByCreatedAtAscIdAsc(10L)).thenReturn(List.of(comment));
    when(postLikes.existsByPostIdAndUserId(10L, 1L)).thenReturn(true);
    when(commentLikes.existsByCommentIdAndUserId(20L, 1L)).thenReturn(true);

    CommunityDto.PostDetailResponse response = service().detail(user, 10L);

    assertThat(response.post().likedByMe()).isTrue();
    assertThat(response.comments()).hasSize(1);
    assertThat(response.comments().getFirst().likedByMe()).isTrue();

    User anotherUser = UserFixture.activeUser();
    ReflectionTestUtils.setField(anotherUser, "id", 2L);
    CommunityDto.PostDetailResponse anotherResponse = service().detail(anotherUser, 10L);
    assertThat(anotherResponse.post().likedByMe()).isFalse();
    assertThat(anotherResponse.comments().getFirst().likedByMe()).isFalse();
  }

  @Test
  void replyToReplyUsesTopLevelParent() {
    User user = UserFixture.activeUser();
    Post post = Post.create(user, PostType.INFO, "title", "body", List.of(),
        null, null, null, null, null);
    ReflectionTestUtils.setField(post, "id", 10L);
    Comment parent = Comment.create(post, user, null, "parent");
    ReflectionTestUtils.setField(parent, "id", 20L);
    Comment reply = Comment.create(post, user, parent, "reply");
    ReflectionTestUtils.setField(reply, "id", 21L);
    when(posts.findById(10L)).thenReturn(Optional.of(post));
    when(comments.findById(21L)).thenReturn(Optional.of(reply));
    when(comments.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CommunityDto.CommentResponse response = service().createComment(user, 10L,
        new CommunityDto.CreateCommentRequest("nested", 21L));

    assertThat(response.parentId()).isEqualTo(20L);
  }

  @Test
  void deletingPostDeletesDependentRowsFirst() {
    User user = UserFixture.activeUser();
    ReflectionTestUtils.setField(user, "id", 1L);
    Post post = Post.create(user, PostType.INFO, "title", "body", List.of(),
        null, null, null, null, null);
    ReflectionTestUtils.setField(post, "id", 10L);
    when(posts.findById(10L)).thenReturn(Optional.of(post));

    service().deletePost(user, 10L);

    InOrder order = inOrder(commentLikes, comments, postLikes, posts);
    order.verify(commentLikes).deleteByCommentPostId(10L);
    order.verify(comments).deleteByPostIdAndParentIsNotNull(10L);
    order.verify(comments).flush();
    order.verify(comments).deleteByPostId(10L);
    order.verify(postLikes).deleteByPostId(10L);
    order.verify(posts).delete(post);
  }

  @Test
  void companionAuthorMayHaveOnlyOnePost() {
    User user = UserFixture.activeUser();
    ReflectionTestUtils.setField(user, "id", 1L);
    when(posts.existsByAuthorIdAndType(1L, PostType.COMPANION)).thenReturn(true);

    assertThatThrownBy(() -> service().create(user,
        new CommunityDto.CreatePostRequest(PostType.COMPANION, "동행", null,
            List.of(), null, 1L, java.time.LocalDate.now(), 3, "부산")))
        .isInstanceOf(BusinessException.class)
        .extracting(error -> ((BusinessException) error).getErrorCode())
        .isEqualTo(CommunityErrorCode.ACTIVE_COMPANION_EXISTS);
  }

  private CommunityService service() {
    return new CommunityService(posts, comments, postLikes, commentLikes, trips, days, items,
        concerts, chatService);
  }
}
