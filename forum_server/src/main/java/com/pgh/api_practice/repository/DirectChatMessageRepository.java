package com.pgh.api_practice.repository;

import com.pgh.api_practice.entity.DirectChatMessage;
import com.pgh.api_practice.entity.DirectChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DirectChatMessageRepository
        extends JpaRepository<DirectChatMessage, Long> {

    /**
     * 채팅방 마지막 메시지 1건
     * (채팅방 목록에서 사용)
     */
    Optional<DirectChatMessage> findTopByChatRoomOrderByCreatedTimeDesc(
            DirectChatRoom chatRoom
    );

    /**
     * 채팅방 메시지 목록 (페이징, 최신순)
     */
    Page<DirectChatMessage> findByChatRoomOrderByCreatedTimeDesc(
            DirectChatRoom chatRoom,
            Pageable pageable
    );

    /**
     * 안 읽은 메시지 수 (read_status 기준)
     *
     * unread =
     *  sender_id != :userId
     *  AND (lastReadMessageId IS NULL OR m.id > lastReadMessageId)
     */
    @Query("""
        select count(m)
        from DirectChatMessage m
        where m.chatRoom = :room
          and m.senderId <> :userId
          and (
                :lastReadMessageId is null
                or m.id > :lastReadMessageId
          )
    """)
    long countUnreadMessages(
            @Param("room") DirectChatRoom room,
            @Param("userId") Long userId,
            @Param("lastReadMessageId") Long lastReadMessageId
    );
}