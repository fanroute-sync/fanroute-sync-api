package com.fanroute.sync.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.broker.SubscriptionRegistry;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.fanroute.sync.domain.auth.config.AdminAuthoritiesConverter;
import com.fanroute.sync.domain.chat.config.ChatStompInterceptor;
import com.fanroute.sync.domain.chat.config.ChatWebSocketConfig;
import com.fanroute.sync.domain.chat.controller.ChatController;
import com.fanroute.sync.domain.chat.controller.ChatMessageController;
import com.fanroute.sync.domain.chat.entity.ChatMemberRole;
import com.fanroute.sync.domain.chat.entity.ChatRoom;
import com.fanroute.sync.domain.chat.entity.ChatRoomMember;
import com.fanroute.sync.domain.chat.repository.ChatRoomMemberRepository;
import com.fanroute.sync.domain.chat.repository.ChatRoomRepository;
import com.fanroute.sync.domain.chat.service.ChatService;
import com.fanroute.sync.domain.community.entity.Post;
import com.fanroute.sync.domain.community.entity.PostType;
import com.fanroute.sync.domain.community.repository.CommentRepository;
import com.fanroute.sync.domain.community.repository.PostRepository;
import com.fanroute.sync.domain.notification.dto.NotificationDto;
import com.fanroute.sync.domain.notification.entity.NotificationType;
import com.fanroute.sync.domain.notification.service.NotificationService;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.repository.UserRepository;
import com.fanroute.sync.domain.user.service.CurrentUserResolver;
import com.fanroute.sync.domain.user.service.UserService;
import com.fanroute.sync.global.common.exception.GlobalExceptionHandler;
import com.fanroute.sync.global.config.JpaAuditingConfig;
import com.fanroute.sync.global.config.SecurityConfig;
import com.fanroute.sync.support.TestContainerConfig;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    classes = {ChatWebSocketIntegrationTest.TestApplication.class, ChatWebSocketConfig.class},
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "cors.allowed-origins=http://localhost",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.data.redis.password="
    })
class ChatWebSocketIntegrationTest {
  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private static final String OWNER_TOKEN = "owner-token";
  private static final String MEMBER_TOKEN = "member-token";

  @LocalServerPort int port;

  @MockitoBean(name = "jwtDecoder") JwtDecoder jwtDecoder;
  @MockitoBean AdminAuthoritiesConverter authoritiesConverter;
  @MockitoBean CurrentUserResolver currentUserResolver;
  @MockitoBean UserService userService;
  @MockitoBean NotificationService notificationService;

  @jakarta.annotation.Resource UserRepository users;
  @jakarta.annotation.Resource PostRepository posts;
  @jakarta.annotation.Resource ChatRoomRepository rooms;
  @jakarta.annotation.Resource ChatRoomMemberRepository members;
  @jakarta.annotation.Resource(name = "simpleBrokerMessageHandler")
  SimpleBrokerMessageHandler broker;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private WebSocketStompClient stompClient;
  private StompSession ownerSession;
  private StompSession memberSession;

  private User owner;
  private User member;
  private Long roomId;

  @BeforeEach
  void setUp() {
    owner = users.saveAndFlush(User.create("owner", AuthProvider.GOOGLE, "owner-google"));
    member = users.saveAndFlush(User.create("member", AuthProvider.GOOGLE, "member-google"));
    Post post = posts.saveAndFlush(Post.create(owner, PostType.COMPANION, "부산 공연 동행", null,
        List.of(), null, null, LocalDate.now(), 2, "부산"));
    ChatRoom room = rooms.saveAndFlush(ChatRoom.createCompanion(post, owner));
    members.saveAllAndFlush(List.of(
        ChatRoomMember.create(room, owner, ChatMemberRole.OWNER),
        ChatRoomMember.create(room, member, ChatMemberRole.MEMBER)));
    roomId = room.getId();

    when(jwtDecoder.decode(OWNER_TOKEN)).thenReturn(jwt(OWNER_TOKEN, owner.getId()));
    when(jwtDecoder.decode(MEMBER_TOKEN)).thenReturn(jwt(MEMBER_TOKEN, member.getId()));
    when(authoritiesConverter.resolveAuthorities(any())).thenReturn(List.of());
    when(currentUserResolver.getCurrentUser(any())).thenAnswer(invocation ->
        invocation.<Jwt>getArgument(0).getSubject().equals(owner.getId().toString()) ? owner : member);
    when(userService.getAccessibleUser(owner.getId())).thenReturn(owner);
    when(userService.getAccessibleUser(member.getId())).thenReturn(member);
    when(notificationService.notifyChatMessage(any(), any(), any(), any())).thenReturn(
        new NotificationDto.NotificationResponse(1L, NotificationType.CHAT_MESSAGE, "채팅",
            "새 메시지", roomId, Instant.now(), null));

    stompClient = new WebSocketStompClient(new StandardWebSocketClient());
  }

  @AfterEach
  void tearDown() {
    if (ownerSession != null && ownerSession.isConnected()) ownerSession.disconnect();
    if (memberSession != null && memberSession.isConnected()) memberSession.disconnect();
    if (stompClient != null) stompClient.stop();
  }

  @Test
  void twoMembersExchangeMessagesAndReadHistoryAndReceipts() throws Exception {
    HttpResponse<String> unauthorized = httpClient.send(
        HttpRequest.newBuilder(historyUri()).GET().build(), HttpResponse.BodyHandlers.ofString());
    assertThat(unauthorized.statusCode()).isEqualTo(401);
    ownerSession = connect(OWNER_TOKEN);
    memberSession = connect(MEMBER_TOKEN);

    BlockingQueue<JsonNode> ownerMessages = new LinkedBlockingQueue<>();
    BlockingQueue<JsonNode> memberMessages = new LinkedBlockingQueue<>();
    BlockingQueue<JsonNode> ownerReads = new LinkedBlockingQueue<>();
    BlockingQueue<JsonNode> memberReads = new LinkedBlockingQueue<>();
    SubscriptionBarrier subscriptions = new SubscriptionBarrier(broker.getSubscriptionRegistry(), 4);
    broker.setSubscriptionRegistry(subscriptions);
    subscribe(ownerSession, "/user/queue/chat.rooms/" + roomId, ownerMessages);
    subscribe(memberSession, "/user/queue/chat.rooms/" + roomId, memberMessages);
    subscribe(ownerSession, "/user/queue/chat.rooms/" + roomId + "/reads", ownerReads);
    subscribe(memberSession, "/user/queue/chat.rooms/" + roomId + "/reads", memberReads);
    assertThat(subscriptions.await()).isTrue();

    send(ownerSession, "첫 번째 메시지");
    JsonNode firstForOwner = take(ownerMessages);
    JsonNode firstForMember = take(memberMessages);
    assertThat(firstForOwner.path("content").asText()).isEqualTo("첫 번째 메시지");
    assertThat(firstForOwner.path("senderId").asLong()).isEqualTo(owner.getId());
    assertThat(firstForMember.path("id").asLong()).isEqualTo(firstForOwner.path("id").asLong());
    assertThat(firstForMember.path("senderId").asLong()).isEqualTo(owner.getId());

    send(memberSession, "두 번째 메시지");
    JsonNode secondForOwner = take(ownerMessages);
    JsonNode secondForMember = take(memberMessages);
    assertThat(secondForOwner.path("content").asText()).isEqualTo("두 번째 메시지");
    assertThat(secondForOwner.path("senderId").asLong()).isEqualTo(member.getId());
    assertThat(secondForMember.path("id").asLong()).isEqualTo(secondForOwner.path("id").asLong());
    assertThat(secondForMember.path("senderId").asLong()).isEqualTo(member.getId());

    HttpResponse<String> history = httpClient.send(HttpRequest.newBuilder(historyUri())
        .header("Authorization", "Bearer " + OWNER_TOKEN)
        .GET()
        .build(), HttpResponse.BodyHandlers.ofString());
    assertThat(history.statusCode()).isEqualTo(200);
    JsonNode historyMessages = objectMapper.readTree(history.body()).path("data").path("messages");
    assertThat(historyMessages).hasSize(2);
    assertThat(historyMessages.get(0).path("content").asText()).isEqualTo("첫 번째 메시지");
    assertThat(historyMessages.get(1).path("content").asText()).isEqualTo("두 번째 메시지");

    long lastMessageId = historyMessages.get(1).path("id").asLong();
    HttpResponse<String> read = httpClient.send(HttpRequest.newBuilder(
        URI.create("http://localhost:" + port + "/api/v1/chat/rooms/" + roomId + "/read"))
        .header("Authorization", "Bearer " + OWNER_TOKEN)
        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .method("PATCH", HttpRequest.BodyPublishers.ofString(
            "{\"lastReadMessageId\":" + lastMessageId + "}"))
        .build(), HttpResponse.BodyHandlers.ofString());
    assertThat(read.statusCode()).isEqualTo(200);
    assertReadReceipt(take(ownerReads), lastMessageId);
    assertReadReceipt(take(memberReads), lastMessageId);
    assertThat(members.findByChatRoomIdAndUserId(roomId, owner.getId()).orElseThrow()
        .getLastReadMessageId()).isEqualTo(lastMessageId);
  }

  private StompSession connect(String token) throws Exception {
    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + token);
    WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
    handshakeHeaders.setOrigin("http://localhost");
    return stompClient.connectAsync(
        URI.create("ws://localhost:" + port + "/ws/chat"),
        handshakeHeaders,
        connectHeaders,
        new StompSessionHandlerAdapter() {})
        .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
  }

  private void subscribe(StompSession session, String destination, BlockingQueue<JsonNode> frames) {
    session.subscribe(destination, new JsonFrameHandler(frames));
  }

  private void send(StompSession session, String content) {
    StompHeaders headers = new StompHeaders();
    headers.setDestination("/app/chat.rooms/" + roomId + "/messages");
    headers.setContentType(MediaType.APPLICATION_JSON);
    session.send(headers, ("{\"content\":\"" + content + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private JsonNode take(BlockingQueue<JsonNode> frames) throws InterruptedException {
    JsonNode frame = frames.poll(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    assertThat(frame).isNotNull();
    return frame;
  }

  private void assertReadReceipt(JsonNode receipt, long lastMessageId) {
    assertThat(receipt.path("roomId").asLong()).isEqualTo(roomId);
    assertThat(receipt.path("userId").asLong()).isEqualTo(owner.getId());
    assertThat(receipt.path("lastReadMessageId").asLong()).isEqualTo(lastMessageId);
  }

  private URI historyUri() {
    return URI.create("http://localhost:" + port + "/api/v1/chat/rooms/" + roomId + "/messages");
  }

  private Jwt jwt(String token, Long userId) {
    Instant now = Instant.now();
    return Jwt.withTokenValue(token)
        .header("alg", "none")
        .subject(userId.toString())
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .build();
  }

  private final class JsonFrameHandler implements StompFrameHandler {
    private final BlockingQueue<JsonNode> frames;

    private JsonFrameHandler(BlockingQueue<JsonNode> frames) {
      this.frames = frames;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
      return byte[].class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
      try {
        frames.add(objectMapper.readTree((byte[]) payload));
      } catch (Exception exception) {
        throw new AssertionError("STOMP JSON frame could not be parsed", exception);
      }
    }
  }

  private static final class SubscriptionBarrier implements SubscriptionRegistry {
    private final SubscriptionRegistry delegate;
    private final CountDownLatch latch;

    private SubscriptionBarrier(SubscriptionRegistry delegate, int expectedSubscriptions) {
      this.delegate = delegate;
      this.latch = new CountDownLatch(expectedSubscriptions);
    }

    @Override
    public void registerSubscription(Message<?> message) {
      delegate.registerSubscription(message);
      latch.countDown();
    }

    @Override
    public void unregisterSubscription(Message<?> message) {
      delegate.unregisterSubscription(message);
    }

    @Override
    public void unregisterAllSubscriptions(String sessionId) {
      delegate.unregisterAllSubscriptions(sessionId);
    }

    @Override
    public MultiValueMap<String, String> findSubscriptions(Message<?> message) {
      return delegate.findSubscriptions(message);
    }

    private boolean await() throws InterruptedException {
      return latch.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @EntityScan("com.fanroute.sync.domain")
  @EnableJpaRepositories(basePackageClasses = {
      ChatRoomRepository.class,
      CommentRepository.class,
      UserRepository.class
  })
  @Import({
      TestContainerConfig.class,
      JpaAuditingConfig.class,
      SecurityConfig.class,
      ChatStompInterceptor.class,
      ChatService.class,
      ChatController.class,
      ChatMessageController.class,
      GlobalExceptionHandler.class
  })
  static class TestApplication {}
}
