-- 게시글 좋아요 테이블 생성 (수정 버전)
-- 
-- 오류 해결 방법:
-- 1. 먼저 아래 쿼리로 posts와 users 테이블의 id 타입을 확인하세요:
--    SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'posts' AND COLUMN_NAME = 'id';
--    SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'id';
--
-- 2. 확인된 타입에 따라:
--    - INT인 경우: 이 스크립트 사용 (post_id와 user_id를 INT로)
--    - BIGINT인 경우: add_post_likes_table_bigint.sql 사용

-- posts.id와 users.id가 INT인 경우
CREATE TABLE IF NOT EXISTS post_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    create_datetime DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_post_user (post_id, user_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
