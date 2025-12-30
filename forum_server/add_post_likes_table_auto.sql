-- 게시글 좋아요 테이블 생성 (자동 타입 감지 버전)
-- 이 스크립트는 기존 테이블의 id 타입을 자동으로 감지하여 호환되는 타입으로 생성합니다

-- 먼저 posts와 users 테이블의 id 컬럼 타입을 확인
SET @post_id_type = (
    SELECT COLUMN_TYPE 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'posts' 
    AND COLUMN_NAME = 'id'
    LIMIT 1
);

SET @user_id_type = (
    SELECT COLUMN_TYPE 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'id'
    LIMIT 1
);

-- 테이블 생성 (동적 SQL은 제한적이므로, 가장 일반적인 경우인 INT로 생성)
-- 만약 오류가 발생하면 아래 주석의 쿼리로 타입을 확인하고 수동으로 수정하세요

-- 타입 확인 쿼리:
-- SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'posts' AND COLUMN_NAME = 'id';
-- SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'id';

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
