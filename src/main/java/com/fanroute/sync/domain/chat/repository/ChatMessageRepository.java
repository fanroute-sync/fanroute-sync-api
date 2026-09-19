package com.fanroute.sync.domain.chat.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fanroute.sync.domain.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
  List<ChatMessage> findByChatRoomIdOrderByIdDesc(Long roomId, Pageable pageable);
  List<ChatMessage> findByChatRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long id, Pageable pageable);
  boolean existsByIdAndChatRoomId(Long id, Long roomId);
  long countByChatRoomIdAndIdGreaterThan(Long roomId, Long id);
  void deleteByChatRoomId(Long roomId);
}
