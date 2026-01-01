package com.pgh.api_practice.service;

import com.pgh.api_practice.dto.GroupChatMessageDTO;
import com.pgh.api_practice.entity.GroupChatMessage;
import com.pgh.api_practice.entity.GroupChatRoom;
import com.pgh.api_practice.entity.MessageRead;
import com.pgh.api_practice.entity.Users;
import com.pgh.api_practice.exception.ApplicationUnauthorizedException;
import com.pgh.api_practice.exception.ResourceNotFoundException;
import com.pgh.api_practice.repository.GroupChatMessageRepository;
import com.pgh.api_practice.repository.GroupChatRoomRepository;
import com.pgh.api_practice.repository.GroupMemberRepository;
import com.pgh.api_practice.repository.MessageReadRepository;
import com.pgh.api_practice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketChatService {

    private final GroupChatMessageRepository messageRepository;
    private final GroupChatRoomRepository roomRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MessageReadRepository readRepository;
    private final UserRepository userRepository;

    /** 메시지 저장 및 DTO 반환 */
    @Transactional
    public GroupChatMessageDTO saveAndGetMessage(Long groupId, Long roomId, String messageText, String username, Long replyToMessageId) {
        log.info("saveAndGetMessage 시작: groupId={}, roomId={}, messageText={}, username={}", 
                groupId, roomId, messageText, username);
        
        if (username == null || username.isEmpty()) {
            log.error("username이 null이거나 비어있습니다.");
            throw new ApplicationUnauthorizedException("인증이 필요합니다.");
        }
        
        Users currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없습니다: username={}", username);
                    return new ResourceNotFoundException("사용자를 찾을 수 없습니다.");
                });
        
        log.info("현재 사용자: username={}, id={}", currentUser.getUsername(), currentUser.getId());

        GroupChatRoom room = roomRepository.findByIdAndIsDeletedFalse(roomId)
                .orElseThrow(() -> {
                    log.error("채팅방을 찾을 수 없습니다: roomId={}", roomId);
                    return new ResourceNotFoundException("채팅방을 찾을 수 없습니다.");
                });
        
        log.info("채팅방 조회 완료: roomId={}, name={}, isAdminRoom={}", roomId, room.getName(), room.isAdminRoom());

        // 모임 멤버인지 확인
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUser.getId());
        log.info("모임 멤버 확인: groupId={}, userId={}, isMember={}", groupId, currentUser.getId(), isMember);
        
        if (!isMember) {
            log.error("모임 멤버가 아닙니다: groupId={}, userId={}", groupId, currentUser.getId());
            throw new ApplicationUnauthorizedException("모임 멤버만 메시지를 전송할 수 있습니다.");
        }

        // 관리자방은 관리자만 접근 가능
        if (room.isAdminRoom()) {
            Optional<com.pgh.api_practice.entity.GroupMember> member = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUser.getId());
            boolean isAdmin = member.isPresent() && member.get().isAdmin();
            log.info("관리자방 접근 확인: isAdminRoom={}, isAdmin={}", room.isAdminRoom(), isAdmin);
            
            if (!isAdmin) {
                log.error("관리자가 아닙니다: groupId={}, userId={}", groupId, currentUser.getId());
                throw new ApplicationUnauthorizedException("관리자만 관리자방에 메시지를 전송할 수 있습니다.");
            }
        }

        // 답장할 메시지 조회
        GroupChatMessage replyToMessage = null;
        if (replyToMessageId != null) {
            replyToMessage = messageRepository.findById(replyToMessageId)
                    .orElseThrow(() -> new ResourceNotFoundException("답장할 메시지를 찾을 수 없습니다."));
            // 같은 채팅방의 메시지인지 확인
            if (!replyToMessage.getChatRoom().getId().equals(roomId)) {
                throw new IllegalArgumentException("답장할 메시지가 같은 채팅방에 없습니다.");
            }
        }

        GroupChatMessage message = GroupChatMessage.builder()
                .chatRoom(room)
                .user(currentUser)
                .message(messageText)
                .replyToMessage(replyToMessage)
                .readCount(0)
                .build();

        log.info("메시지 저장 시작: message={}", message);
        GroupChatMessage saved = messageRepository.save(message);
        log.info("메시지 저장 완료: messageId={}", saved.getId());
        
        GroupChatMessageDTO dto = convertToDTO(saved, groupId);
        log.info("DTO 변환 완료: messageId={}, username={}, nickname={}", dto.getId(), dto.getUsername(), dto.getNickname());
        
        return dto;
    }

    /** 메시지 읽음 처리 */
    @Transactional
    public void markMessageAsRead(Long messageId, String username) {
        GroupChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("메시지를 찾을 수 없습니다."));

        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        // 이미 읽음 상태인지 확인
        if (!readRepository.existsByMessageIdAndUserId(messageId, user.getId())) {
            MessageRead read = MessageRead.builder()
                    .message(message)
                    .user(user)
                    .build();
            readRepository.save(read);

            // 읽음 수 업데이트
            message.setReadCount(message.getReadCount() + 1);
            messageRepository.save(message);
        }
    }

    /** 읽음 수 조회 */
    public int getReadCount(Long messageId) {
        return readRepository.countByMessageId(messageId);
    }

    /** 메시지를 DTO로 변환 */
    private GroupChatMessageDTO convertToDTO(GroupChatMessage message, Long groupId) {
        // 모임 주인과 관리자 목록 확인
        com.pgh.api_practice.entity.Group group = message.getChatRoom().getGroup();
        Long ownerId = group.getOwner().getId();
        List<Long> adminIds = new ArrayList<>();
        adminIds.add(ownerId); // 모임 주인은 항상 관리자
        
        try {
            List<com.pgh.api_practice.entity.GroupMember> members = groupMemberRepository.findByGroupId(groupId);
            adminIds.addAll(members.stream()
                    .filter(com.pgh.api_practice.entity.GroupMember::isAdmin)
                    .map(m -> m.getUser().getId())
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.warn("관리자 목록 조회 실패: {}", e.getMessage());
        }

        Long userId = message.getUser().getId();
        boolean isAdmin = adminIds.contains(userId);

        // 답장 정보 처리
        GroupChatMessageDTO.ReplyToMessageInfo replyToMessageInfo = null;
        if (message.getReplyToMessage() != null) {
            GroupChatMessage replyTo = message.getReplyToMessage();
            // 답장한 메시지의 작성자 정보 조회
            Users replyToUser = replyTo.getUser();
            // 답장한 메시지 작성자의 displayName 조회
            String displayName = null;
            try {
                Optional<com.pgh.api_practice.entity.GroupMember> replyToMember = groupMemberRepository
                        .findByGroupIdAndUserId(groupId, replyToUser.getId());
                if (replyToMember.isPresent() && replyToMember.get().getDisplayName() != null) {
                    displayName = replyToMember.get().getDisplayName();
                }
            } catch (Exception e) {
                log.warn("답장 메시지 작성자의 displayName 조회 실패: {}", e.getMessage());
            }
            
            replyToMessageInfo = GroupChatMessageDTO.ReplyToMessageInfo.builder()
                    .id(replyTo.getId())
                    .message(replyTo.getMessage())
                    .username(replyToUser.getUsername())
                    .nickname(replyToUser.getNickname())
                    .displayName(displayName)
                    .profileImageUrl(replyToUser.getProfileImageUrl())
                    .build();
        }

        // 현재 메시지 작성자의 displayName 조회
        String displayName = null;
        try {
            Optional<com.pgh.api_practice.entity.GroupMember> member = groupMemberRepository
                    .findByGroupIdAndUserId(groupId, userId);
            if (member.isPresent() && member.get().getDisplayName() != null) {
                displayName = member.get().getDisplayName();
            }
        } catch (Exception e) {
            log.warn("현재 메시지 작성자의 displayName 조회 실패: {}", e.getMessage());
        }

        return GroupChatMessageDTO.builder()
                .id(message.getId())
                .message(message.getMessage())
                .username(message.getUser().getUsername())
                .nickname(message.getUser().getNickname())
                .displayName(displayName)
                .profileImageUrl(message.getUser().getProfileImageUrl())
                .isAdmin(isAdmin)
                .createdTime(message.getCreatedTime())
                .readCount(message.getReadCount())
                .replyToMessageId(message.getReplyToMessage() != null ? message.getReplyToMessage().getId() : null)
                .replyToMessage(replyToMessageInfo)
                .build();
    }
}
