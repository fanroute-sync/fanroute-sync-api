package com.fanroute.sync.domain.community.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.fanroute.sync.domain.community.dto.CommunityDto;
import com.fanroute.sync.domain.community.entity.PostType;
import com.fanroute.sync.domain.community.exception.CommunityErrorCode;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RequestMapping("/api/v1/community/posts")
@Tag(name = "커뮤니티", description = "Fan Route 게시글, 댓글, 좋아요")
@SecurityRequirement(name = "bearerAuth")
public interface CommunityApi {
  @GetMapping
  @Operation(summary = "게시글 목록 조회", description = "유형·검색어·지역 필터와 최신순/인기순 정렬")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "조회 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = CommunityErrorCode.class, names = "INVALID_POST")
  ResponseEntity<ApiResponse<Page<CommunityDto.PostResponse>>> list(
      @RequestParam(required = false) PostType type, @RequestParam(required = false) String query,
      @RequestParam(required = false) String region,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);

  @GetMapping("/{postId}")
  @Operation(summary = "게시글 상세와 댓글 조회")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "조회 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = CommunityErrorCode.class, names = "POST_NOT_FOUND")
  ResponseEntity<ApiResponse<CommunityDto.PostDetailResponse>> detail(@PathVariable Long postId);

  @PostMapping
  @Operation(summary = "게시글 작성")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "201", description = "작성 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = CommunityErrorCode.class,
      names = {"INVALID_POST", "ACTIVE_COMPANION_EXISTS"})
  ResponseEntity<ApiResponse<CommunityDto.PostResponse>> create(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody CommunityDto.CreatePostRequest request);

  @DeleteMapping("/{postId}")
  @Operation(summary = "게시글 삭제")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = CommunityErrorCode.class, names = {"POST_NOT_FOUND", "FORBIDDEN"})
  ResponseEntity<ApiResponse<Void>> delete(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @PathVariable Long postId);

  @PutMapping("/{postId}/like")
  @Operation(summary = "게시글 좋아요")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "좋아요 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = CommunityErrorCode.class, names = "POST_NOT_FOUND")
  ResponseEntity<ApiResponse<Void>> like(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @PathVariable Long postId);

  @DeleteMapping("/{postId}/like")
  @Operation(summary = "게시글 좋아요 취소")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "취소 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = CommunityErrorCode.class, names = "POST_NOT_FOUND")
  ResponseEntity<ApiResponse<Void>> unlike(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @PathVariable Long postId);

  @PostMapping("/{postId}/comments")
  @Operation(summary = "댓글 또는 답글 작성")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "201", description = "작성 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = CommunityErrorCode.class,
      names = {"POST_NOT_FOUND", "COMMENT_NOT_FOUND"})
  ResponseEntity<ApiResponse<CommunityDto.CommentResponse>> comment(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @PathVariable Long postId, @Valid @RequestBody CommunityDto.CreateCommentRequest request);

  @DeleteMapping("/{postId}/comments/{commentId}")
  @Operation(summary = "댓글과 하위 답글 삭제")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = CommunityErrorCode.class,
      names = {"POST_NOT_FOUND", "COMMENT_NOT_FOUND", "FORBIDDEN"})
  ResponseEntity<ApiResponse<Void>> deleteComment(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @PathVariable Long postId, @PathVariable Long commentId);

  @PutMapping("/{postId}/comments/{commentId}/like")
  @Operation(summary = "댓글 좋아요")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "좋아요 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = CommunityErrorCode.class,
      names = {"POST_NOT_FOUND", "COMMENT_NOT_FOUND"})
  ResponseEntity<ApiResponse<Void>> likeComment(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @PathVariable Long postId, @PathVariable Long commentId);

  @DeleteMapping("/{postId}/comments/{commentId}/like")
  @Operation(summary = "댓글 좋아요 취소")
  @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200", description = "취소 성공", useReturnTypeSchema = true))
  @ApiErrorCodeExamples(type = CommunityErrorCode.class,
      names = {"POST_NOT_FOUND", "COMMENT_NOT_FOUND"})
  ResponseEntity<ApiResponse<Void>> unlikeComment(
      @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
      @PathVariable Long postId, @PathVariable Long commentId);
}
