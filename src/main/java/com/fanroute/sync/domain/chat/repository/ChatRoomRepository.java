package com.fanroute.sync.domain.chat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.fanroute.sync.domain.chat.entity.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
  Optional<ChatRoom> findByCompanionPostId(Long postId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select room from ChatRoom room where room.id = :roomId")
  Optional<ChatRoom> findByIdForUpdate(@Param("roomId") Long roomId);
}
