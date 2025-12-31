package com.pgh.api_practice.repository;

import com.pgh.api_practice.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByIdAndIsDeletedFalse(Long id);
    Page<Group> findByIsDeletedFalseOrderByCreatedTimeDesc(Pageable pageable);
}
