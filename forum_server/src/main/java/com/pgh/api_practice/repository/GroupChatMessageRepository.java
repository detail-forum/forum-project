package com.pgh.api_practice.repository;

import com.pgh.api_practice.entity.GroupChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupChatMessageRepository extends JpaRepository<GroupChatMessage, Long> {
    Page<GroupChatMessage> findByChatRoomIdAndIsDeletedFalseOrderByCreatedTimeDesc(Long chatRoomId, Pageable pageable);
    
    @Query("SELECT gcm FROM GroupChatMessage gcm WHERE gcm.chatRoom.id = :chatRoomId AND gcm.isDeleted = false ORDER BY gcm.createdTime DESC")
    List<GroupChatMessage> findRecentMessages(@Param("chatRoomId") Long chatRoomId, Pageable pageable);
}
