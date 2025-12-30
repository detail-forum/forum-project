-- posts 테이블의 body 컬럼을 TEXT 타입으로 변경
ALTER TABLE posts MODIFY COLUMN body TEXT NOT NULL;

-- posts 테이블의 title 컬럼 길이를 500으로 변경
ALTER TABLE posts MODIFY COLUMN title VARCHAR(500) NOT NULL;

-- posts 테이블에 profile_image_url 컬럼 추가
ALTER TABLE posts ADD COLUMN profile_image_url VARCHAR(500) NULL;
