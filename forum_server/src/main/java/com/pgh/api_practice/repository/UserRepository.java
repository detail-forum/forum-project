package com.pgh.api_practice.repository;

import com.pgh.api_practice.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {
    /** username으로 사용자 조회 (중복 시 첫 번째 결과만 반환) */
    @Query(value = "SELECT * FROM users WHERE username = :username AND is_deleted = false ORDER BY id ASC LIMIT 1", nativeQuery = true)
    Optional<Users> findFirstByUsernameNative(@Param("username") String username);
    
    /** username으로 사용자 조회 (기존 메서드와 호환성을 위해 유지) */
    default Optional<Users> findByUsername(String username) {
        return findFirstByUsernameNative(username);
    }
}
