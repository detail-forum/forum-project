package com.pgh.api_practice.repository;

import com.pgh.api_practice.entity.DirectChatReadStatus;
import com.pgh.api_practice.entity.DirectChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DirectChatReadStatusRepository
        extends JpaRepository<DirectChatReadStatus, Long> {

    Optional<DirectChatReadStatus> findByChatRoomAndUserId(
            DirectChatRoom chatRoom,
            Long userId
    );
}