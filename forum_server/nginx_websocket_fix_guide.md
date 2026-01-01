# Nginx WebSocket 설정 수정 가이드

## 문제
`/ws/info` 경로가 프론트엔드 페이지로 보이는 경우, Nginx가 `/ws` 경로를 프론트엔드로 프록시하고 있습니다.

## 해결 방법

### 1. `^~` 수정자 사용 (권장)

`/ws` location에 `^~` 수정자를 추가하여 우선순위를 높입니다:

```nginx
# ⚠️ 중요: ^~ 수정자로 우선순위 보장
location ^~ /ws {
    proxy_pass http://127.0.0.1:8081;
    proxy_http_version 1.1;
    
    # WebSocket 업그레이드 헤더 (필수)
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    
    # 기본 프록시 헤더
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # WebSocket 타임아웃 설정
    proxy_read_timeout 86400;
    proxy_send_timeout 86400;
    proxy_connect_timeout 60;
    
    # 버퍼링 비활성화
    proxy_buffering off;
}
```

### 2. Location 순서 확인

Nginx location 블록 순서:
1. `=` (정확한 매칭) - 최우선
2. `^~` (prefix 매칭, 정규식 중단) - 두 번째 우선
3. `~` 또는 `~*` (정규식 매칭)
4. 일반 prefix 매칭 (가장 긴 매칭이 우선)

**중요**: `/ws` 경로는 `/` (프론트엔드)보다 **위에** 위치해야 합니다.

### 3. 현재 설정에서 수정할 부분

기존 설정:
```nginx
location /ws {
    proxy_pass http://localhost:8081;
    # ... 나머지 설정
}
```

수정된 설정:
```nginx
location ^~ /ws {
    proxy_pass http://127.0.0.1:8081;
    proxy_http_version 1.1;
    
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    proxy_read_timeout 86400;
    proxy_send_timeout 86400;
    proxy_connect_timeout 60;
    proxy_buffering off;
}
```

### 4. 적용 및 테스트

```bash
# 설정 파일 문법 검사
nginx -t

# 오류가 없으면 재시작
nginx -s reload
```

### 5. 테스트

브라우저 개발자 도구에서:
1. Network 탭 열기
2. `/ws/info` 요청 확인
3. Status가 200 OK인지 확인
4. Response가 JSON 형식인지 확인 (HTML이 아닌)

## 문제 해결 체크리스트

- [ ] `location ^~ /ws` 사용 (^~ 수정자 추가)
- [ ] `/ws` 경로가 `/` 경로보다 위에 위치
- [ ] `proxy_pass`가 `http://127.0.0.1:8081`로 설정
- [ ] `Upgrade`와 `Connection` 헤더 설정
- [ ] `proxy_buffering off` 설정
- [ ] Nginx 재시작 완료
- [ ] 백엔드가 8081 포트에서 실행 중
