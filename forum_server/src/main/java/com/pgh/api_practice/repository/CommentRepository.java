package com.pgh.api_practice.repository;

import com.pgh.api_practice.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 게시글의 댓글 목록 조회 (삭제되지 않은 것만, 고정된 댓글 우선, 그 다음 생성일시 순)
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.isDeleted = false AND c.parentComment IS NULL ORDER BY c.isPinned DESC, c.createdTime ASC")
    List<Comment> findAllByPostIdAndNotDeletedAndNoParent(@Param("postId") Long postId);

    // 부모 댓글의 대댓글 목록 조회
    @Query("SELECT c FROM Comment c WHERE c.parentComment.id = :parentId AND c.isDeleted = false ORDER BY c.createdTime ASC")
    List<Comment> findAllByParentCommentIdAndNotDeleted(@Param("parentId") Long parentId);

    // 게시글의 모든 댓글 조회 (대댓글 포함)
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.isDeleted = false")
    List<Comment> findAllByPostIdAndNotDeleted(@Param("postId") Long postId);

    // 댓글과 작성자, 게시글 정보를 함께 조회
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.post WHERE c.id = :id")
    Optional<Comment> findByIdWithUserAndPost(@Param("id") Long id);
}

