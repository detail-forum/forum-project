-- group_chat_messages 테이블에 reply_to_message_id 컬럼 추가
-- 이미 컬럼이 존재하면 추가하지 않음

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'group_chat_messages'
               AND COLUMN_NAME = 'reply_to_message_id');

SET @sqlstmt := IF(@exist = 0,
    'ALTER TABLE group_chat_messages ADD COLUMN reply_to_message_id BIGINT DEFAULT NULL,
     ADD CONSTRAINT fk_group_chat_message_reply_to FOREIGN KEY (reply_to_message_id) REFERENCES group_chat_messages(id) ON DELETE SET NULL',
    'SELECT "reply_to_message_id column already exists"');

PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
