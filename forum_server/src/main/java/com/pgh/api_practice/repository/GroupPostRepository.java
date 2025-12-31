package com.pgh.api_practice.repository;

import com.pgh.api_practice.entity.GroupPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupPostRepository extends JpaRepository<GroupPost, Long> {
    Optional<GroupPost> findByIdAndIsDeletedFalse(Long id);
    Page<GroupPost> findByGroupIdAndIsDeletedFalseOrderByCreatedTimeDesc(Long groupId, Pageable pageable);
    
    @Modifying
    @Query("UPDATE GroupPost gp SET gp.views = gp.views + 1 WHERE gp.id = :id")
    void incrementViews(@Param("id") Long id);
}
