package com.fanroute.sync.domain.chat.config;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import com.fanroute.sync.domain.chat.service.ChatService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatStompInterceptor implements ChannelInterceptor {
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtDecoder jwtDecoder;
  private final ChatService chatService;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String authorization = accessor.getFirstNativeHeader("Authorization");
      if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
        throw new IllegalArgumentException("STOMP access token is required");
      }
      Jwt jwt = jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()));
      accessor.setUser(new UsernamePasswordAuthenticationToken(jwt.getSubject(), null));
    }
    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
      Principal principal = accessor.getUser();
      if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
          && "/user/queue/notifications".equals(accessor.getDestination()) && principal != null) {
        return message;
      }
      Long roomId = roomId(accessor.getDestination());
      if (principal == null || roomId == null) throw new IllegalArgumentException("Invalid chat destination");
      chatService.requireActiveMember(roomId, Long.valueOf(principal.getName()));
    }
    return message;
  }

  private Long roomId(String destination) {
    if (destination == null) return null;
    String prefix = destination.startsWith("/app/chat.rooms/") ? "/app/chat.rooms/"
        : destination.startsWith("/user/queue/chat.rooms/") ? "/user/queue/chat.rooms/" : null;
    if (prefix == null) return null;
    String suffix = destination.substring(prefix.length());
    int separator = suffix.indexOf('/');
    String id = separator < 0 ? suffix : suffix.substring(0, separator);
    try { return Long.valueOf(id); } catch (NumberFormatException ignored) { return null; }
  }
}
