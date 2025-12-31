package com.pgh.api_practice.service;

import com.pgh.api_practice.dto.*;
import com.pgh.api_practice.entity.*;
import com.pgh.api_practice.exception.ApplicationUnauthorizedException;
import com.pgh.api_practice.exception.ResourceNotFoundException;
import com.pgh.api_practice.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class GroupPostService {

    private final GroupPostRepository groupPostRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    /** 현재 사용자 가져오기 */
    private Users getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    /** 모임 활동 게시물 생성 */
    @Transactional
    public Long createGroupPost(Long groupId, CreateGroupPostDTO dto) {
        Users currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new ApplicationUnauthorizedException("인증이 필요합니다.");
        }

        Group group = groupRepository.findByIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("모임을 찾을 수 없습니다."));

        // 모임 멤버인지 확인
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUser.getId())) {
            throw new ApplicationUnauthorizedException("모임 멤버만 게시물을 작성할 수 있습니다.");
        }

        GroupPost post = GroupPost.builder()
                .group(group)
                .title(dto.getTitle())
                .body(dto.getBody())
                .user(currentUser)
                .profileImageUrl(dto.getProfileImageUrl())
                .build();

        GroupPost created = groupPostRepository.save(post);
        return created.getId();
    }

    /** 모임 활동 게시물 목록 조회 */
    @Transactional(readOnly = true)
    public Page<GroupPostListDTO> getGroupPostList(Long groupId, Pageable pageable) {
        Group group = groupRepository.findByIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("모임을 찾을 수 없습니다."));

        Page<GroupPost> posts = groupPostRepository.findByGroupIdAndIsDeletedFalseOrderByCreatedTimeDesc(groupId, pageable);

        List<GroupPostListDTO> postList = posts.getContent().stream().map(post -> {
            LocalDateTime updateTime = post.getUpdatedTime();
            if (updateTime == null || updateTime.isBefore(post.getCreatedTime()) ||
                updateTime.isBefore(LocalDateTime.of(1970, 1, 2, 0, 0))) {
                updateTime = post.getCreatedTime();
            }

            return GroupPostListDTO.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .body(post.getBody())
                    .username(post.getUser().getUsername())
                    .nickname(post.getUser().getNickname())
                    .Views(String.valueOf(post.getViews()))
                    .createDateTime(post.getCreatedTime())
                    .updateDateTime(updateTime)
                    .profileImageUrl(post.getProfileImageUrl())
                    .build();
        }).collect(Collectors.toList());

        return new PageImpl<>(postList, pageable, posts.getTotalElements());
    }

    /** 모임 활동 게시물 상세 조회 */
    @Transactional
    public GroupPostDetailDTO getGroupPostDetail(Long groupId, Long postId) {
        Group group = groupRepository.findByIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("모임을 찾을 수 없습니다."));

        GroupPost post = groupPostRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시물을 찾을 수 없습니다."));

        // 조회수 증가
        groupPostRepository.incrementViews(postId);
        post = groupPostRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시물을 찾을 수 없습니다."));

        Users currentUser = getCurrentUser();
        boolean isAuthor = false;
        boolean canEdit = false;
        boolean canDelete = false;

        if (currentUser != null) {
            isAuthor = post.getUser().getId().equals(currentUser.getId());
            Optional<GroupMember> member = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUser.getId());
            boolean isAdmin = member.isPresent() && member.get().isAdmin();
            canEdit = isAuthor;
            canDelete = isAuthor || isAdmin;
        }

        LocalDateTime updateTime = post.getUpdatedTime();
        if (updateTime == null || updateTime.isBefore(post.getCreatedTime()) ||
            updateTime.isBefore(LocalDateTime.of(1970, 1, 2, 0, 0))) {
            updateTime = post.getCreatedTime();
        }

        return GroupPostDetailDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .body(post.getBody())
                .username(post.getUser().getUsername())
                .nickname(post.getUser().getNickname())
                .Views(String.valueOf(post.getViews()))
                .createDateTime(post.getCreatedTime())
                .updateDateTime(updateTime)
                .profileImageUrl(post.getProfileImageUrl())
                .isAuthor(isAuthor)
                .canEdit(canEdit)
                .canDelete(canDelete)
                .build();
    }

    /** 모임 활동 게시물 수정 */
    @Transactional
    public void updateGroupPost(Long groupId, Long postId, CreateGroupPostDTO dto) {
        Users currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new ApplicationUnauthorizedException("인증이 필요합니다.");
        }

        Group group = groupRepository.findByIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("모임을 찾을 수 없습니다."));

        GroupPost post = groupPostRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시물을 찾을 수 없습니다."));

        // 작성자만 수정 가능
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new ApplicationUnauthorizedException("작성자만 수정할 수 있습니다.");
        }

        post.setTitle(dto.getTitle());
        post.setBody(dto.getBody());
        if (dto.getProfileImageUrl() != null) {
            post.setProfileImageUrl(dto.getProfileImageUrl());
        }
        groupPostRepository.save(post);
    }

    /** 모임 활동 게시물 삭제 */
    @Transactional
    public void deleteGroupPost(Long groupId, Long postId) {
        Users currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new ApplicationUnauthorizedException("인증이 필요합니다.");
        }

        Group group = groupRepository.findByIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("모임을 찾을 수 없습니다."));

        GroupPost post = groupPostRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시물을 찾을 수 없습니다."));

        // 작성자 또는 관리자만 삭제 가능
        boolean isAuthor = post.getUser().getId().equals(currentUser.getId());
        Optional<GroupMember> member = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUser.getId());
        boolean isAdmin = member.isPresent() && member.get().isAdmin();

        if (!isAuthor && !isAdmin) {
            throw new ApplicationUnauthorizedException("작성자 또는 관리자만 삭제할 수 있습니다.");
        }

        post.setDeleted(true);
        groupPostRepository.save(post);
    }
}
