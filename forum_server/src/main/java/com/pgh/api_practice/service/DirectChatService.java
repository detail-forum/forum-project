package com.pgh.api_practice.service;

import com.pgh.api_practice.dto.CreateDirectMessageDTO;
import com.pgh.api_practice.dto.DirectChatMessageDTO;
import com.pgh.api_practice.dto.DirectChatMessagePageDTO;
import com.pgh.api_practice.dto.DirectChatRoomDTO;
import com.pgh.api_practice.entity.DirectChatMessage;
import com.pgh.api_practice.entity.DirectChatReadStatus;
import com.pgh.api_practice.entity.DirectChatRoom;
import com.pgh.api_practice.entity.Users;
import com.pgh.api_practice.exception.ResourceNotFoundException;
import com.pgh.api_practice.repository.DirectChatMessageRepository;
import com.pgh.api_practice.repository.DirectChatReadStatusRepository;
import com.pgh.api_practice.repository.DirectChatRoomRepository;
import com.pgh.api_practice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectChatService {

    private final DirectChatMessageRepository messageRepository;
    private final DirectChatReadStatusRepository readStatusRepository;
    private final DirectChatRoomRepository roomRepository;
    private final UserRepository userRepository;

    /** 1대1 채팅방 생성 또는 조회 */
    @Transactional
    public DirectChatRoomDTO getOrCreateRoom(Long otherUserId) {
        if (otherUserId == null) {
            throw new IllegalArgumentException("상대 사용자 ID는 필수입니다.");
        }
        
        Users me = getCurrentUser();
        
        // 자기 자신과는 채팅방 생성 불가
        if (me.getId().equals(otherUserId)) {
            throw new IllegalArgumentException("자기 자신과는 채팅방을 만들 수 없습니다.");
        }
        
        Users other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("상대 사용자를 찾을 수 없습니다: " + otherUserId));

        Long user1 = Math.min(me.getId(), other.getId());
        Long user2 = Math.max(me.getId(), other.getId());

        DirectChatRoom room = roomRepository
                .findByUser1IdAndUser2Id(user1, user2)
                .orElseGet(() -> {
                    DirectChatRoom newRoom = new DirectChatRoom(user1, user2);
                    return roomRepository.save(newRoom);
                });

        return toRoomDTO(room, me.getId());
    }

    /** 내 1대1 채팅방 목록 조회 */
    public List<DirectChatRoomDTO> getMyRooms() {
        Users me = getCurrentUser();

        try {
            return roomRepository.findMyRooms(me.getId())
                    .stream()
                    .map(room -> {
                        try {
                            return toRoomDTO(room, me.getId());
                        } catch (Exception e) {
                            // 개별 채팅방 변환 실패 시 로그만 남기고 건너뛰기
                            log.error("채팅방 변환 실패 (roomId: {}): {}", room.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .toList();
        } catch (Exception e) {
            log.error("1대1 채팅방 목록 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("채팅방 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /* =========================
       내부 변환 로직
       ========================= */

    private DirectChatRoomDTO toRoomDTO(DirectChatRoom room, Long myUserId) {
        Long otherUserId = room.getOtherUserId(myUserId);
        Users other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("상대 사용자를 찾을 수 없습니다: " + otherUserId));

        DirectChatMessage lastMessage =
                messageRepository.findTopByChatRoomOrderByCreatedTimeDesc(room).orElse(null);

        DirectChatReadStatus readStatus =
                readStatusRepository.findByChatRoomAndUserId(room, myUserId)
                        .orElse(null);

        Long lastReadMessageId =
                (readStatus != null && readStatus.getLastReadMessage() != null)
                        ? readStatus.getLastReadMessage().getId()
                        : null;

        int unreadCount = (int) messageRepository.countUnreadMessages(
                room,
                myUserId,
                lastReadMessageId
        );

        return DirectChatRoomDTO.builder()
                .id(room.getId())
                .otherUserId(other.getId())
                .otherUsername(other.getUsername())
                .otherNickname(other.getNickname())
                .otherProfileImageUrl(other.getProfileImageUrl())
                .lastMessage(lastMessage != null ? lastMessage.getMessage() : null)
                .lastMessageTime(lastMessage != null ? lastMessage.getCreatedTime() : null)
                .unreadCount(unreadCount)
                .updatedTime(room.getUpdatedTime())
                .build();
    }

    private Users getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        if (username == null || "anonymousUser".equals(username)) {
            throw new ResourceNotFoundException("인증이 필요합니다.");
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("인증 사용자 정보를 찾을 수 없습니다: " + username));
    }

    @Transactional
    public DirectChatMessagePageDTO getMessages(
            Long chatRoomId,
            int page,
            int size
    ) {
        Users me = getCurrentUser();

        DirectChatRoom room = roomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅방이 존재하지 않습니다: " + chatRoomId));

        // 멤버 검증
        if (!room.getUser1Id().equals(me.getId())
                && !room.getUser2Id().equals(me.getId())) {
            throw new ResourceNotFoundException("채팅방 접근 권한이 없습니다.");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<DirectChatMessage> pageResult =
                messageRepository.findByChatRoomOrderByCreatedTimeDesc(room, pageable);

        DirectChatMessage latestVisibleMessage =
                pageResult.getContent().stream()
                        .max(Comparator.comparing(DirectChatMessage::getId))
                        .orElse(null);

        DirectChatReadStatus myReadStatus =
                readStatusRepository.findByChatRoomAndUserId(room, me.getId())
                        .orElseGet(() ->
                                readStatusRepository.save(
                                        new DirectChatReadStatus(room, me.getId())
                                )
                        );

        if (latestVisibleMessage != null) {
            DirectChatMessage prev = myReadStatus.getLastReadMessage();
            if (prev == null || latestVisibleMessage.getId() > prev.getId()) {
                myReadStatus.updateRead(latestVisibleMessage);
            }
        }

        Long myLastReadMessageId =
                myReadStatus.getLastReadMessage() != null
                        ? myReadStatus.getLastReadMessage().getId()
                        : null;

        Long otherUserId = room.getOtherUserId(me.getId());

        List<DirectChatMessageDTO> content = pageResult
                .map(message ->
                        toMessageDTO(
                                message,
                                me.getId(),
                                myLastReadMessageId
                        )
                )
                .getContent();

        return new DirectChatMessagePageDTO(
                content,
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    private DirectChatMessageDTO toMessageDTO(
            DirectChatMessage message,
            Long myUserId,
            Long myLastReadMessageId
    ) {
        Users sender = userRepository.findById(message.getSenderId())
                .orElseThrow(() -> new ResourceNotFoundException("발신자를 찾을 수 없습니다: " + message.getSenderId()));

        boolean isRead;

        if (message.getSenderId().equals(myUserId)) {
            isRead = true;
        } else {
            isRead = myLastReadMessageId != null
                    && myLastReadMessageId >= message.getId();
        }

        return DirectChatMessageDTO.builder()
                .id(message.getId())
                .roomId(message.getChatRoom().getId())
                .senderId(sender.getId())
                .username(sender.getUsername())
                .nickname(sender.getNickname())
                .profileImageUrl(sender.getProfileImageUrl())
                .message(message.getMessage())
                .createdTime(message.getCreatedTime())
                .messageType(message.getMessageType())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .isRead(isRead)
                .build();
    }

    @Transactional
    public DirectChatMessageDTO sendMessage(
            Long chatRoomId,
            CreateDirectMessageDTO dto
    ) {
        Users me = getCurrentUser();

        DirectChatRoom room = roomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅방이 존재하지 않습니다: " + chatRoomId));

        if (!room.getUser1Id().equals(me.getId())
                && !room.getUser2Id().equals(me.getId())) {
            throw new ResourceNotFoundException("채팅방 접근 권한이 없습니다.");
        }

        validateByType(dto);

        DirectChatMessage message = new DirectChatMessage(
                room,
                me.getId(),
                dto.getMessage(),
                DirectChatMessage.MessageType.valueOf(dto.getMessageType().name()),
                dto.getFileUrl(),
                dto.getFileName(),
                dto.getFileSize()
        );

        DirectChatMessage saved = messageRepository.save(message);

        DirectChatReadStatus readStatus = readStatusRepository
                .findByChatRoomAndUserId(room, me.getId())
                .orElseGet(() ->
                        readStatusRepository.save(
                                new DirectChatReadStatus(room, me.getId())
                        )
                );

        readStatus.updateRead(saved);
        readStatusRepository.save(readStatus);

        publishToWebSocket(saved);

        return toMessageDTOForSend(saved);
    }

    private DirectChatMessageDTO toMessageDTOForSend(DirectChatMessage message) {
        Users sender = userRepository.findById(message.getSenderId())
                .orElseThrow(() -> new ResourceNotFoundException("발신자를 찾을 수 없습니다: " + message.getSenderId()));

        return DirectChatMessageDTO.builder()
                .id(message.getId())
                .roomId(message.getChatRoom().getId())
                .senderId(sender.getId())
                .message(message.getMessage())
                .username(sender.getUsername())
                .nickname(sender.getNickname())
                .profileImageUrl(sender.getProfileImageUrl())
                .createdTime(message.getCreatedTime())
                .messageType(message.getMessageType())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .isRead(true)
                .build();
    }

    private void validateByType(CreateDirectMessageDTO req) {
        if (req.getMessageType() == null) {
            throw new IllegalArgumentException("messageType은 필수입니다.");
        }

        switch (req.getMessageType()) {
            case TEXT -> {
                if (req.getMessage() == null || req.getMessage().isBlank()) {
                    throw new IllegalArgumentException("TEXT 타입은 message가 필요합니다.");
                }
            }
            case IMAGE -> {
                if (req.getFileUrl() == null || req.getFileUrl().isBlank()) {
                    throw new IllegalArgumentException("IMAGE 타입은 fileUrl이 필요합니다.");
                }
            }
            case FILE -> {
                if (req.getFileUrl() == null || req.getFileUrl().isBlank()
                        || req.getFileName() == null || req.getFileName().isBlank()
                        || req.getFileSize() == null || req.getFileSize() <= 0) {
                    throw new IllegalArgumentException("FILE 타입은 fileUrl, fileName, fileSize가 필요합니다.");
                }
            }
        }
    }

    private void validateCreateMessage(CreateDirectMessageDTO dto) {
        switch (dto.getMessageType()) {
            case IMAGE -> {
                if (dto.getFileUrl() == null)
                    throw new IllegalArgumentException("IMAGE 타입은 fileUrl이 필요합니다.");
            }
            case FILE -> {
                if (dto.getFileUrl() == null
                        || dto.getFileName() == null
                        || dto.getFileSize() == null)
                    throw new IllegalArgumentException("FILE 타입은 fileUrl, fileName, fileSize가 필요합니다.");
            }
            case TEXT -> {
                // message만 있으면 OK
            }
        }
    }

    // WebSocket 전송 스텁 (실제 구현은 WebSocket/SimpMessagingTemplate)
    private void publishToWebSocket(DirectChatMessage message) {
        // TODO: SimpMessagingTemplate.convertAndSend("/topic/direct/" + roomId, payload)
    }
}