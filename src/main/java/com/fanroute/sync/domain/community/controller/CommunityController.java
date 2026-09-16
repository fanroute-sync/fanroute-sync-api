package com.fanroute.sync.domain.community.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.community.dto.CommunityDto;
import com.fanroute.sync.domain.community.entity.PostType;
import com.fanroute.sync.domain.community.service.CommunityService;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.global.common.response.ApiResponse;
import com.fanroute.sync.global.common.response.SuccessCode;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CommunityController implements CommunityApi {
  private final CommunityService service;
  private final CurrentUserResolver currentUser;

  @Override
  public ResponseEntity<ApiResponse<Page<CommunityDto.PostResponse>>> list(Jwt jwt, PostType type,
      String query, String region, String sort, int page, int size) {
    return ApiResponse.ok(service.list(currentUser.getCurrentUser(jwt), type, query, region,
        sort, page, size)).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<CommunityDto.PostDetailResponse>> detail(Jwt jwt, Long postId) {
    return ApiResponse.ok(service.detail(currentUser.getCurrentUser(jwt), postId)).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<CommunityDto.PostResponse>> create(Jwt jwt,
      CommunityDto.CreatePostRequest request) {
    return ApiResponse.of(SuccessCode.CREATED,
        service.create(currentUser.getCurrentUser(jwt), request)).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> delete(Jwt jwt, Long postId) {
    service.deletePost(currentUser.getCurrentUser(jwt), postId);
    return ApiResponse.<Void>ok().toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> like(Jwt jwt, Long postId) {
    service.likePost(currentUser.getCurrentUser(jwt), postId);
    return ApiResponse.<Void>ok().toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> unlike(Jwt jwt, Long postId) {
    service.unlikePost(currentUser.getCurrentUser(jwt), postId);
    return ApiResponse.<Void>ok().toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<CommunityDto.CommentResponse>> comment(Jwt jwt,
      Long postId, CommunityDto.CreateCommentRequest request) {
    return ApiResponse.of(SuccessCode.CREATED,
        service.createComment(currentUser.getCurrentUser(jwt), postId, request)).toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> deleteComment(Jwt jwt, Long postId, Long commentId) {
    service.deleteComment(currentUser.getCurrentUser(jwt), postId, commentId);
    return ApiResponse.<Void>ok().toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> likeComment(Jwt jwt, Long postId, Long commentId) {
    service.likeComment(currentUser.getCurrentUser(jwt), postId, commentId);
    return ApiResponse.<Void>ok().toResponseEntity();
  }

  @Override
  public ResponseEntity<ApiResponse<Void>> unlikeComment(Jwt jwt, Long postId, Long commentId) {
    service.unlikeComment(currentUser.getCurrentUser(jwt), postId, commentId);
    return ApiResponse.<Void>ok().toResponseEntity();
  }
}
