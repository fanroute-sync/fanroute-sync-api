package com.fanroute.sync.domain.community.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.fanroute.sync.domain.community.entity.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CommunityDto {
  private CommunityDto() {}

  public record CreatePostRequest(
      @NotNull PostType type,
      @NotBlank @Size(max = 200) String title,
      @Schema(description = "정보 공유 본문") String content,
      List<@Size(max = 50) String> tags,
      Long tripPlanId,
      Long concertId,
      LocalDate companionDate,
      Integer capacity,
      @Size(max = 100) @Schema(description = "지역 필터용 지역명") String region) {}

  public record PostResponse(Long id, PostType type, String title, String content,
      List<String> tags, Long authorId, String authorNickname, Long concertId,
      String concertTitle, Long tripPlanId, LocalDate companionDate, Integer capacity,
      Integer currentMembers, String region, long likeCount,
      @Schema(description = "현재 로그인 사용자의 게시글 좋아요 여부") boolean likedByMe,
      long commentCount,
      Instant createdAt) {}

  public record CreateCommentRequest(@NotBlank @Size(max = 1000) String content,
      @Schema(description = "답글 대상 댓글 ID. 답글의 답글도 최상위 댓글에 연결") Long parentId) {}

  public record CommentResponse(Long id, Long parentId, Long authorId,
      String authorNickname, String content, long likeCount,
      @Schema(description = "현재 로그인 사용자의 댓글 좋아요 여부") boolean likedByMe,
      Instant createdAt) {}

  public record PostDetailResponse(PostResponse post, List<CommentResponse> comments) {}
}
