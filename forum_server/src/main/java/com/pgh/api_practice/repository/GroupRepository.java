package com.pgh.api_practice.repository;

import com.pgh.api_practice.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByIdAndIsDeletedFalse(Long id);
    Page<Group> findByIsDeletedFalseOrderByCreatedTimeDesc(Pageable pageable);
    
    @Modifying
    @Query("UPDATE Group g SET g.views = g.views + 1 WHERE g.id = :id")
    void incrementViews(@Param("id") Long id);
}
