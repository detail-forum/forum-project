package com.pgh.api_practice.service;

import com.pgh.api_practice.dto.CreatePost;
import com.pgh.api_practice.dto.PatchPostDTO;
import com.pgh.api_practice.dto.PostDetailDTO;
import com.pgh.api_practice.dto.PostListDTO;
import com.pgh.api_practice.entity.Post;
import com.pgh.api_practice.entity.PostLike;
import com.pgh.api_practice.entity.Users;
import com.pgh.api_practice.exception.ApplicationUnauthorizedException;
import com.pgh.api_practice.exception.ResourceNotFoundException;
import com.pgh.api_practice.repository.PostLikeRepository;
import com.pgh.api_practice.repository.PostRepository;
import com.pgh.api_practice.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;

    /** ✅ 게시글 저장 */
    public long savePost(CreatePost dto) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new ApplicationUnauthorizedException("인증이 필요합니다.");
        }
        
        String username = authentication.getName();

        Users author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("유저를 찾을 수 없습니다."));

        Post post = Post.builder()
                .title(dto.getTitle())
                .body(dto.getBody())
                .user(author)
                .profileImageUrl(dto.getProfileImageUrl())
                .build();

        Post created = postRepository.save(post);
        return created.getId();
    }

    /** ✅ 단건 조회 (조회수 증가 포함) */
    @Transactional
    public PostDetailDTO getPostDetail(long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));

        if (post.isDeleted()) {
            throw new ResourceNotFoundException("삭제된 게시글입니다.");
        }

        // 조회수 증가 (updatedTime은 변경하지 않음)
        postRepository.incrementViews(id);
        
        // 조회수 증가 후 다시 조회하여 정확한 값 가져오기
        post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));

        // updateDateTime이 null이거나 유효하지 않은 경우 createDateTime으로 설정
        LocalDateTime updateTime = post.getUpdatedTime();
        if (updateTime == null || updateTime.isBefore(post.getCreatedTime()) || 
            updateTime.isBefore(LocalDateTime.of(1970, 1, 2, 0, 0))) {
            updateTime = post.getCreatedTime();
        }

        // 좋아요 수 조회
        long likeCount = postLikeRepository.countByPostId(post.getId());
        
        // 현재 사용자가 좋아요를 눌렀는지 확인
        boolean isLiked = false;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !"anonymousUser".equals(authentication.getName())) {
            String username = authentication.getName();
            Users user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), user.getId());
            }
        }

        return PostDetailDTO.builder()
                .title(post.getTitle())
                .body(post.getBody())
                .username(post.getUser().getUsername())
                .Views(String.valueOf(post.getViews()))
                .createDateTime(post.getCreatedTime())
                .updateDateTime(updateTime)
                .profileImageUrl(post.getProfileImageUrl())
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }

    /** ✅ 전체 게시글 목록 */
    @Transactional(readOnly = true)
    public Page<PostListDTO> getPostList(Pageable pageable, String sortType) {
        Page<Post> posts;

        if ("RESENT".equalsIgnoreCase(sortType)) {
            posts = postRepository.findAllByIsDeletedFalseOrderByCreatedTimeDesc(pageable);
        } else if ("HITS".equalsIgnoreCase(sortType)) {
            posts = postRepository.findAllByIsDeletedFalseOrderByViewsDesc(pageable);
        } else if ("LIKES".equalsIgnoreCase(sortType)) {
            posts = postRepository.findAllByIsDeletedFalseOrderByLikesDesc(pageable);
        } else {
            posts = postRepository.findAllByIsDeletedFalseOrderByCreatedTimeDesc(pageable);
        }

        return posts.map(post -> {
            // updateDateTime이 null이거나 유효하지 않은 경우 createDateTime으로 설정
            LocalDateTime updateTime = post.getUpdatedTime();
            if (updateTime == null || updateTime.isBefore(post.getCreatedTime()) || 
                updateTime.isBefore(LocalDateTime.of(1970, 1, 2, 0, 0))) {
                updateTime = post.getCreatedTime();
            }
            
            // 좋아요 수 조회
            long likeCount = postLikeRepository.countByPostId(post.getId());
            
            return PostListDTO.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .username(post.getUser().getUsername())
                    .views(post.getViews())
                    .createDateTime(post.getCreatedTime())
                    .updateDateTime(updateTime)
                    .profileImageUrl(post.getProfileImageUrl())
                    .likeCount(likeCount)
                    .build();
        });
    }

    /** ✅ 내 게시글 목록 */
    @Transactional(readOnly = true)
    public Page<PostListDTO> getMyPostList(Pageable pageable, String sortType) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new ApplicationUnauthorizedException("인증이 필요합니다.");
        }
        
        String requestUsername = authentication.getName();

        Users user = userRepository.findByUsername(requestUsername)
                .orElseThrow(() -> new ResourceNotFoundException("유저를 찾을 수 없습니다."));

        Page<Post> posts;

        if ("RESENT".equalsIgnoreCase(sortType)) {
            posts = postRepository.findAllByUserIdAndIsDeletedFalseOrderByCreatedTimeDesc(user.getId(), pageable);
        } else if ("HITS".equalsIgnoreCase(sortType)) {
            posts = postRepository.findAllByUserIdAndIsDeletedFalseOrderByViewsDesc(user.getId(), pageable);
        } else if ("LIKES".equalsIgnoreCase(sortType)) {
            posts = postRepository.findAllByUserIdAndIsDeletedFalseOrderByLikesDesc(user.getId(), pageable);
        } else {
            posts = postRepository.findAllByUserIdAndIsDeletedFalseOrderByCreatedTimeDesc(user.getId(), pageable);
        }

        return posts.map(post -> {
            // updateDateTime이 null이거나 유효하지 않은 경우 createDateTime으로 설정
            LocalDateTime updateTime = post.getUpdatedTime();
            if (updateTime == null || updateTime.isBefore(post.getCreatedTime()) || 
                updateTime.isBefore(LocalDateTime.of(1970, 1, 2, 0, 0))) {
                updateTime = post.getCreatedTime();
            }
            
            // 좋아요 수 조회
            long likeCount = postLikeRepository.countByPostId(post.getId());
            
            return PostListDTO.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .username(post.getUser().getUsername())
                    .views(post.getViews())
                    .createDateTime(post.getCreatedTime())
                    .updateDateTime(updateTime)
                    .profileImageUrl(post.getProfileImageUrl())
                    .likeCount(likeCount)
                    .build();
        });
    }

    /** ✅ 게시글 좋아요 추가/삭제 */
    @Transactional
    public boolean toggleLike(long postId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new ApplicationUnauthorizedException("인증이 필요합니다.");
        }
        
        String username = authentication.getName();
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("유저를 찾을 수 없습니다."));
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));
        
        if (post.isDeleted()) {
            throw new ResourceNotFoundException("삭제된 게시글입니다.");
        }
        
        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, user.getId());
        
        if (existingLike.isPresent()) {
            // 좋아요 취소
            postLikeRepository.delete(existingLike.get());
            return false; // 좋아요 취소됨
        } else {
            // 좋아요 추가
            PostLike like = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(like);
            return true; // 좋아요 추가됨
        }
    }

    /** ✅ 게시글 삭제 */
    public void deletePost(long id) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new ApplicationUnauthorizedException("인증이 필요합니다.");
        }
        
        String requestUserName = authentication.getName();

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getUsername().equals(requestUserName)) {
            throw new ApplicationUnauthorizedException("작성자만 게시글을 삭제할 수 있습니다.");
        }

        post.setDeleted(true);
        postRepository.save(post);
    }

    /** ✅ 게시글 수정 */
    @Transactional
    public void updatePost(long id, PatchPostDTO dto) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new ApplicationUnauthorizedException("인증이 필요합니다.");
        }
        
        String requestUserName = authentication.getName();

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다."));

        if (post.isDeleted()) {
            throw new ResourceNotFoundException("삭제된 게시글입니다.");
        }

        if (!post.getUser().getUsername().equals(requestUserName)) {
            throw new ApplicationUnauthorizedException("작성자만 게시글을 수정할 수 있습니다.");
        }

        // 내용이 실제로 변경되었는지 확인
        boolean isModified = false;
        if (dto.getBody() != null && !dto.getBody().equals(post.getBody())) {
            post.setBody(dto.getBody());
            isModified = true;
        }
        if (dto.getTitle() != null && !dto.getTitle().equals(post.getTitle())) {
            post.setTitle(dto.getTitle());
            isModified = true;
        }
        if (dto.getProfileImageUrl() != null && !dto.getProfileImageUrl().equals(post.getProfileImageUrl())) {
            post.setProfileImageUrl(dto.getProfileImageUrl());
            isModified = true;
        }

        // 변경사항이 있을 때만 저장
        if (isModified) {
            // 명시적으로 수정 시간 설정 (확실하게 업데이트되도록)
            LocalDateTime now = LocalDateTime.now();
            
            // 1. 엔티티에 직접 설정
            post.setUpdatedTime(now);
            postRepository.save(post);
            
            // 2. @Modifying 쿼리로도 업데이트 (이중 보장)
            postRepository.updateModifiedTime(id, now);
            
            // 플러시하여 DB에 즉시 반영
            postRepository.flush();
        }
    }
}
