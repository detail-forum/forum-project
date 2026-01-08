package com.pgh.api_practice.repository;

import com.pgh.api_practice.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuthRepository extends JpaRepository<Users, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    Optional<Users> findByUsername(String username);
    Optional<Users> findByEmail(String email);
    Optional<Users> findByEmailVerificationToken(String token);
    
    /** 사용자 검색 (username 또는 nickname으로 검색) */
    @Query("SELECT u FROM Users u WHERE " +
           "u.isDeleted = false AND u.emailVerified = true AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.nickname) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY u.nickname ASC")
    List<Users> searchUsers(@Param("query") String query);
}