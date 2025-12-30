-- 게시글 좋아요 테이블 생성
-- 
-- 사용 방법:
-- 1. 먼저 check_table_types.sql을 실행하여 posts.id와 users.id의 실제 타입을 확인하세요
-- 2. 확인된 타입에 따라 적절한 스크립트를 선택:
--    - INT인 경우: 이 스크립트 사용 (아래 post_id와 user_id를 INT로)
--    - BIGINT인 경우: add_post_likes_table_bigint.sql 사용
--
-- 또는 아래 스크립트를 직접 실행해보고 오류가 발생하면 다른 버전을 시도하세요.

-- posts.id와 users.id가 INT인 경우 (가장 일반적)
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
