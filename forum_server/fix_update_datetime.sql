-- 기존 게시글의 update_datetime이 null이거나 잘못된 값인 경우 수정
-- update_datetime이 null이거나 create_datetime과 같거나, 1970년 이전인 경우 create_datetime으로 설정

UPDATE posts 
SET update_datetime = create_datetime 
WHERE update_datetime IS NULL 
   OR update_datetime = create_datetime 
   OR update_datetime < '1970-01-02 00:00:00';

-- 수정된 게시글만 update_datetime이 create_datetime과 다르도록 설정
-- (실제로 수정된 게시글은 이미 올바른 값이 있을 것이므로 이 쿼리는 주로 null이나 잘못된 값을 수정)

