package com.fanroute.sync.domain.community.exception;

import org.springframework.http.HttpStatus;
import com.fanroute.sync.global.common.response.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommunityErrorCode implements BaseCode {
  POST_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_POST_NOT_FOUND", "게시글을 찾을 수 없습니다."),
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "COMMUNITY_FORBIDDEN", "작성자만 삭제할 수 있습니다."),
  INVALID_POST(HttpStatus.BAD_REQUEST, "COMMUNITY_INVALID_POST", "게시글 입력이 올바르지 않습니다."),
  INVALID_COMMENT(HttpStatus.BAD_REQUEST, "COMMUNITY_INVALID_COMMENT", "댓글 입력이 올바르지 않습니다."),
  ACTIVE_COMPANION_EXISTS(HttpStatus.CONFLICT, "COMMUNITY_ACTIVE_COMPANION_EXISTS", "활성 동행 모집 글은 하나만 작성할 수 있습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
