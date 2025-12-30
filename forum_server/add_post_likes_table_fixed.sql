-- 게시글 좋아요 테이블 생성
-- 먼저 기존 테이블의 id 컬럼 타입을 확인하고 호환되는 타입으로 생성

-- 1단계: 기존 테이블의 id 컬럼 타입 확인 (실행 후 결과 확인)
-- SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'posts' AND COLUMN_NAME = 'id';
-- SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'id';

-- 2단계: post_likes 테이블 생성
-- posts.id와 users.id가 INT인 경우를 대비하여 INT로 생성
-- 만약 BIGINT라면 아래 스크립트에서 INT를 BIGINT로 변경하세요

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
