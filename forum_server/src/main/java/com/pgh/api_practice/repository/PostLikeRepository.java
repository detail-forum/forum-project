package com.pgh.api_practice.repository;

import com.pgh.api_practice.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    
    void deleteByPostIdAndUserId(Long postId, Long userId);
    
    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);
}
