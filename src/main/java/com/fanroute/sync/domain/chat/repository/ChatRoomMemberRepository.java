package com.fanroute.sync.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.fanroute.sync.domain.chat.entity.ChatRoomMember;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
  Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long roomId, Long userId);
  boolean existsByChatRoomIdAndUserIdAndLeftAtIsNull(Long roomId, Long userId);
  long countByChatRoomIdAndLeftAtIsNull(Long roomId);
  long countByChatRoomIdAndLeftAtIsNullAndLastReadMessageIdGreaterThanEqual(Long roomId,
      Long lastReadMessageId);
  List<ChatRoomMember> findByUserIdAndLeftAtIsNullOrderByChatRoomLastMessageAtDesc(Long userId);
  List<ChatRoomMember> findByChatRoomIdAndLeftAtIsNull(Long roomId);
  void deleteByChatRoomId(Long roomId);
}
