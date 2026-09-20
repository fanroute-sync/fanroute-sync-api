package com.fanroute.sync.domain.chat.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.fanroute.sync.domain.chat.entity.ChatRoomMember;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
  Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long roomId, Long userId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("""
      update ChatRoomMember member
      set member.lastReadMessageId = :messageId, member.lastReadAt = :readAt,
          member.updatedAt = :readAt
      where member.id = :memberId and member.leftAt is null
        and (member.lastReadMessageId is null or member.lastReadMessageId < :messageId)
      """)
  int advanceReadCursor(@Param("memberId") Long memberId, @Param("messageId") Long messageId,
      @Param("readAt") Instant readAt);

  @Query("select member.lastReadMessageId from ChatRoomMember member where member.id = :memberId")
  Long findLastReadMessageId(@Param("memberId") Long memberId);

  boolean existsByChatRoomIdAndUserIdAndLeftAtIsNull(Long roomId, Long userId);
  long countByChatRoomIdAndLeftAtIsNull(Long roomId);
  long countByChatRoomIdAndLeftAtIsNullAndLastReadMessageIdGreaterThanEqual(Long roomId,
      Long lastReadMessageId);
  List<ChatRoomMember> findByUserIdAndLeftAtIsNullOrderByChatRoomLastMessageAtDesc(Long userId);
  List<ChatRoomMember> findByChatRoomIdAndLeftAtIsNull(Long roomId);
  void deleteByChatRoomId(Long roomId);
}
