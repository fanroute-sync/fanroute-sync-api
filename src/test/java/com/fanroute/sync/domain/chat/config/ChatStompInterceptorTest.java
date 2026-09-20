package com.fanroute.sync.domain.chat.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.fanroute.sync.domain.chat.exception.ChatErrorCode;
import com.fanroute.sync.domain.chat.service.ChatService;
import com.fanroute.sync.global.common.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ChatStompInterceptorTest {

  @Mock
  private JwtDecoder decoder;
  @Mock
  private ChatService chatService;

  @Test
  @DisplayName("CONNECT 인증 사용자를 원본 메시지와 WebSocket 세션 콜백에 반영한다")
  void authenticatesOriginalConnectMessage() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setNativeHeader("Authorization", "Bearer test-token");
    AtomicReference<Principal> sessionUser = new AtomicReference<>();
    accessor.setUserChangeCallback(sessionUser::set);
    Message<byte[]> message = message(accessor);
    when(decoder.decode("test-token"))
        .thenReturn(Jwt.withTokenValue("test-token").header("alg", "HS256").subject("1").build());

    assertThat(interceptor().preSend(message, null)).isSameAs(message);

    assertThat(accessor.getUser()).isNotNull();
    assertThat(accessor.getUser().getName()).isEqualTo("1");
    assertThat(sessionUser.get()).isSameAs(accessor.getUser());
  }

  @Test
  @DisplayName("CONNECT 토큰이 없으면 연결을 거부한다")
  void rejectsMissingToken() {
    Message<byte[]> message = message(StompHeaderAccessor.create(StompCommand.CONNECT));

    assertThatThrownBy(() -> interceptor().preSend(message, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("유효하지 않은 CONNECT 토큰은 사용자를 인증하지 않는다")
  void rejectsInvalidToken() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setNativeHeader("Authorization", "Bearer invalid-token");
    Message<byte[]> message = message(accessor);
    when(decoder.decode("invalid-token")).thenThrow(new BadJwtException("Invalid token"));

    assertThatThrownBy(() -> interceptor().preSend(message, null))
        .isInstanceOf(BadJwtException.class);
    assertThat(accessor.getUser()).isNull();
  }

  @ParameterizedTest
  @EnumSource(value = StompCommand.class, names = {"SEND", "SUBSCRIBE"})
  @DisplayName("CONNECT 인증 없이 전송이나 구독을 시도하면 거부한다")
  void rejectsUnauthenticatedFrames(StompCommand command) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    accessor.setDestination(command == StompCommand.SEND
        ? "/app/chat.rooms/20/messages" : "/user/queue/chat.rooms/20");
    Message<byte[]> message = message(accessor);

    assertThatThrownBy(() -> interceptor().preSend(message, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @EnumSource(value = StompCommand.class, names = {"SEND", "SUBSCRIBE"})
  @DisplayName("메시지 전송과 구독은 활성 참여자인지 확인한다")
  void authorizesActiveMember(StompCommand command) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    accessor.setUser(() -> "1");
    accessor.setDestination(command == StompCommand.SEND
        ? "/app/chat.rooms/20/messages" : "/user/queue/chat.rooms/20");
    Message<byte[]> message = message(accessor);

    assertThat(interceptor().preSend(message, null)).isSameAs(message);

    verify(chatService).requireActiveMember(20L, 1L);
  }

  @ParameterizedTest
  @EnumSource(value = StompCommand.class, names = {"SEND", "SUBSCRIBE"})
  @DisplayName("JWT 사용자가 방 참여자가 아니면 전송과 구독을 거부한다")
  void rejectsNonMember(StompCommand command) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    accessor.setUser(() -> "3");
    accessor.setDestination(command == StompCommand.SEND
        ? "/app/chat.rooms/20/messages" : "/user/queue/chat.rooms/20/reads");
    Message<byte[]> message = message(accessor);
    doThrow(new BusinessException(ChatErrorCode.FORBIDDEN))
        .when(chatService).requireActiveMember(20L, 3L);

    assertThatThrownBy(() -> interceptor().preSend(message, null))
        .isInstanceOf(BusinessException.class);
  }

  private Message<byte[]> message(StompHeaderAccessor accessor) {
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private ChatStompInterceptor interceptor() {
    return new ChatStompInterceptor(decoder, chatService);
  }
}
