package com.fanroute.sync.domain.chat.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.chat.dto.ChatDto;
import com.fanroute.sync.domain.chat.entity.ChatMemberRole;
import com.fanroute.sync.domain.chat.entity.ChatMessage;
import com.fanroute.sync.domain.chat.entity.ChatRoom;
import com.fanroute.sync.domain.chat.entity.ChatRoomMember;
import com.fanroute.sync.domain.chat.exception.ChatErrorCode;
import com.fanroute.sync.domain.chat.repository.ChatMessageRepository;
import com.fanroute.sync.domain.chat.repository.ChatRoomMemberRepository;
import com.fanroute.sync.domain.chat.repository.ChatRoomRepository;
import com.fanroute.sync.domain.community.entity.Comment;
import com.fanroute.sync.domain.community.entity.Post;
import com.fanroute.sync.domain.community.entity.PostType;
import com.fanroute.sync.domain.community.repository.CommentRepository;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
  private static final int MAX_MESSAGE_PAGE_SIZE = 100;

  private final ChatRoomRepository rooms;
  private final ChatRoomMemberRepository members;
  private final ChatMessageRepository messages;
  private final CommentRepository comments;

  @Transactional
  public void createCompanionRoom(Post post) {
    ChatRoom room = rooms.save(ChatRoom.createCompanion(post, post.getAuthor()));
    members.save(ChatRoomMember.create(room, post.getAuthor(), ChatMemberRole.OWNER));
  }

  public List<ChatDto.RoomResponse> list(User user) {
    List<ChatRoomMember> joinedRooms =
        members.findByUserIdAndLeftAtIsNullOrderByChatRoomLastMessageAtDesc(user.getId());
    List<Long> lastMessageIds = joinedRooms.stream()
        .map(member -> member.getChatRoom().getLastMessageId())
        .filter(id -> id != null).toList();
    Map<Long, String> lastMessages = new HashMap<>();
    if (!lastMessageIds.isEmpty()) {
      for (ChatMessage message : messages.findAllById(lastMessageIds)) {
        lastMessages.put(message.getId(), message.getContent());
      }
    }
    return joinedRooms.stream()
        .map(member -> toRoom(member, lastMessages.get(member.getChatRoom().getLastMessageId())))
        .toList();
  }

  public ChatDto.MessagePageResponse messages(User user, Long roomId, Long beforeMessageId, int size) {
    requireActiveMemberEntity(roomId, user.getId());
    if (size < 1 || size > MAX_MESSAGE_PAGE_SIZE) throw new BusinessException(ChatErrorCode.INVALID_MESSAGE);
    List<ChatMessage> page = beforeMessageId == null
        ? messages.findByChatRoomIdOrderByIdDesc(roomId, PageRequest.of(0, size + 1))
        : messages.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, beforeMessageId,
            PageRequest.of(0, size + 1));
    boolean hasNext = page.size() > size;
    List<ChatMessage> result = page.subList(0, Math.min(size, page.size()));
    Long next = hasNext ? result.getLast().getId() : null;
    List<ChatDto.MessageResponse> response = result.stream().map(this::toMessage).toList().reversed();
    return new ChatDto.MessagePageResponse(response, hasNext, next);
  }

  @Transactional
  public void accept(User owner, Long roomId, Long commentId) {
    ChatRoom room = rooms.findByIdForUpdate(roomId)
        .orElseThrow(() -> new BusinessException(ChatErrorCode.ROOM_NOT_FOUND));
    if (!room.getCreatedBy().getId().equals(owner.getId())) throw new BusinessException(ChatErrorCode.FORBIDDEN);
    Comment comment = comments.findById(commentId)
        .orElseThrow(() -> new BusinessException(ChatErrorCode.INVALID_COMMENT));
    if (room.getType() != com.fanroute.sync.domain.chat.entity.ChatRoomType.COMPANION
        || comment.getParent() != null || !comment.getPost().getId().equals(room.getCompanionPost().getId())
        || comment.getAuthor().getId().equals(owner.getId())) {
      throw new BusinessException(ChatErrorCode.INVALID_COMMENT);
    }
    ChatRoomMember member = members.findByChatRoomIdAndUserId(roomId, comment.getAuthor().getId())
        .orElse(null);
    if (member != null && member.isActive()) return;
    int capacity = room.getCompanionPost().getCapacity();
    if (members.countByChatRoomIdAndLeftAtIsNull(roomId) >= capacity) {
      throw new BusinessException(ChatErrorCode.CAPACITY_REACHED);
    }
    if (member == null) members.save(ChatRoomMember.create(room, comment.getAuthor(), ChatMemberRole.MEMBER));
    else member.rejoin();
  }

  @Transactional
  public ChatDto.MessageResponse send(User user, Long roomId, String content) {
    ChatRoom room = rooms.findById(roomId)
        .orElseThrow(() -> new BusinessException(ChatErrorCode.ROOM_NOT_FOUND));
    ChatRoomMember senderMember = requireActiveMemberEntity(roomId, user.getId());
    String normalized = content == null ? "" : content.trim();
    if (normalized.isEmpty() || normalized.length() > 1000) {
      throw new BusinessException(ChatErrorCode.INVALID_MESSAGE);
    }
    ChatMessage message = messages.save(ChatMessage.createText(room, user, normalized));
    senderMember.markRead(message.getId(), message.getCreatedAt());
    room.updateLastMessage(message.getId(), message.getCreatedAt());
    return toMessage(message);
  }

  @Transactional
  public ChatDto.ReadReceiptResponse markRead(User user, Long roomId, Long messageId) {
    ChatRoomMember member = requireActiveMemberEntity(roomId, user.getId());
    if (!messages.existsByIdAndChatRoomId(messageId, roomId)) {
      throw new BusinessException(ChatErrorCode.INVALID_MESSAGE);
    }
    member.markRead(messageId, Instant.now());
    return new ChatDto.ReadReceiptResponse(roomId, user.getId(), member.getLastReadMessageId());
  }

  public void requireActiveMember(Long roomId, Long userId) {
    if (!members.existsByChatRoomIdAndUserIdAndLeftAtIsNull(roomId, userId)) {
      throw new BusinessException(ChatErrorCode.FORBIDDEN);
    }
  }

  public List<Long> activeMemberIds(Long roomId) {
    return members.findByChatRoomIdAndLeftAtIsNull(roomId).stream().map(member -> member.getUser().getId())
        .toList();
  }

  public int activeMemberCount(Long companionPostId) {
    return rooms.findByCompanionPostId(companionPostId)
        .map(room -> Math.toIntExact(members.countByChatRoomIdAndLeftAtIsNull(room.getId())))
        .orElse(1);
  }

  @Transactional
  public void deleteCompanionRoom(Long companionPostId) {
    rooms.findByCompanionPostId(companionPostId).ifPresent(room -> {
      messages.deleteByChatRoomId(room.getId());
      members.deleteByChatRoomId(room.getId());
      rooms.delete(room);
    });
  }

  private ChatRoomMember requireActiveMemberEntity(Long roomId, Long userId) {
    return members.findByChatRoomIdAndUserId(roomId, userId)
        .filter(ChatRoomMember::isActive)
        .orElseThrow(() -> new BusinessException(ChatErrorCode.FORBIDDEN));
  }

  private ChatDto.RoomResponse toRoom(ChatRoomMember member, String lastMessage) {
    ChatRoom room = member.getChatRoom();
    Long lastRead = member.getLastReadMessageId();
    long unread = lastRead == null ? messages.countByChatRoomIdAndIdGreaterThan(room.getId(), 0L)
        : messages.countByChatRoomIdAndIdGreaterThan(room.getId(), lastRead);
    return new ChatDto.RoomResponse(room.getId(), room.getCompanionPost().getId(),
        room.getCompanionPost().getTitle(), room.getLastMessageAt(), lastMessage, unread);
  }

  private ChatDto.MessageResponse toMessage(ChatMessage message) {
    return new ChatDto.MessageResponse(message.getId(), message.getChatRoom().getId(),
        message.getSender().getId(), message.getSender().getNickname(), message.getContent(),
        message.getCreatedAt(), members.countByChatRoomIdAndLeftAtIsNullAndLastReadMessageIdGreaterThanEqual(
            message.getChatRoom().getId(), message.getId()));
  }
}
