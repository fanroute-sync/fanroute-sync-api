package com.fanroute.sync.domain.chat.exception;

import org.springframework.http.HttpStatus;
import com.fanroute.sync.global.common.response.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements BaseCode {
  ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "CHAT_FORBIDDEN", "채팅방 참여자만 이용할 수 있습니다."),
  INVALID_MESSAGE(HttpStatus.BAD_REQUEST, "CHAT_INVALID_MESSAGE", "메시지 입력이 올바르지 않습니다."),
  INVALID_COMMENT(HttpStatus.BAD_REQUEST, "CHAT_INVALID_COMMENT", "채택할 수 없는 댓글입니다."),
  CAPACITY_REACHED(HttpStatus.CONFLICT, "CHAT_CAPACITY_REACHED", "채팅방 모집 정원이 찼습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
