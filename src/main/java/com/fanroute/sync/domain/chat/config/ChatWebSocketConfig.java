package com.fanroute.sync.domain.chat.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final ChatStompInterceptor interceptor;

  @Value("${cors.allowed-origins}")
  private List<String> allowedOrigins;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/queue");
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws/chat").setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
  }

  @Override
  public void configureClientInboundChannel(
      org.springframework.messaging.simp.config.ChannelRegistration registration) {
    registration.interceptors(interceptor);
  }
}
